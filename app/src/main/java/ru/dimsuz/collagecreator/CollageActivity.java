package ru.dimsuz.collagecreator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.print.PrintHelper;
import android.support.v4.util.LruCache;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import ru.dimsuz.collagecreator.collage.CollageBuilder;
import ru.dimsuz.collagecreator.collage.CollageLayout;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.network.ImageFetcher;
import ru.dimsuz.collagecreator.network.InstagramClient;
import ru.dimsuz.collagecreator.util.Functions;
import rx.Observable;
import rx.android.lifecycle.LifecycleEvent;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Presents a user with a collage and options to adjust it
 */
public class CollageActivity extends RxCompatActivity {
    private final static int CHOOSE_PHOTOS_REQUEST = 1;
    private static final int DEFAULT_COLLAGE_SIZE = 1200;

    @Inject
    InstagramClient instagramClient;
    @Inject
    LruCache<String, List<ImageInfo>> userImagesCache;
    @Inject
    Map<String,Typeface> typefaceCache;

    @InjectView(R.id.collageView)
    ImageView collageView;
    @InjectView(R.id.progressBar)
    View progressBar;
    @InjectView(R.id.content)
    View contentLayout;
    @InjectView(R.id.button_choose_text)
    TextView choosePhotosButtonText;
    @InjectView(R.id.button_print_text)
    TextView printPhotosButtonText;

    @Nullable
    private Bitmap latestCollageBitmap;
    private UserInfo userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collage);

        CollageCreatorApp.get(this).inject(this);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupActionButtons();

        userInfo = getIntent().getParcelableExtra(Consts.EXTRA_USER_INFO);
        if(userInfo == null || !userInfo.isValid()) {
            throw new RuntimeException("required user info is missing");
        }

        // createCollageViewSizedCollage(userInfo);
        // instead of above, decided to do a higher dimension collage
        // 10x15 photo frame is 1200x1800 (300ppi), and DEFAULT_COLLAGE_SIZE=1200, soo...
        createCollage(userInfo, CollageLayout.SIMPLE_2x2, DEFAULT_COLLAGE_SIZE, Collections.<String>emptyList());
    }

    private void setupActionButtons() {
        choosePhotosButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
        printPhotosButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
    }

    private void createCollageViewSizedCollage(final UserInfo userInfo) {
        // ensure view is measured and laid out, safe to take size
        collageView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        collageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        createCollage(userInfo, CollageLayout.SIMPLE_2x2,
                                Math.min(collageView.getWidth(), collageView.getHeight()),
                                Collections.<String>emptyList());
                    }
                });
    }

    private void createCollage(UserInfo userInfo, final List<RectF> layout, int size, @NotNull List<String> selectedIds) {
        Observable<Bitmap> collageObservable = createCollageObservable(userInfo, layout, size, selectedIds);
        LifecycleObservable.bindUntilLifecycleEvent(lifecycle(), collageObservable, LifecycleEvent.DESTROY)
                .doOnSubscribe(showProgressBar())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(showCollage(), showError());
    }

    private Action0 showProgressBar() {
        return new Action0() {
            @Override
            public void call() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                        contentLayout.setVisibility(View.GONE);
                    }
                });
            }
        };
    }

    @NotNull
    private Action1<Bitmap> showCollage() {
        return new Action1<Bitmap>() {
            @Override
            public void call(Bitmap collageBitmap) {
                Timber.d("got collage made: %dx%d", collageBitmap.getWidth(), collageBitmap.getHeight());
                collageView.setImageBitmap(collageBitmap);
                progressBar.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
                latestCollageBitmap = collageBitmap;
            }
        };
    }

    @NotNull
    private Action1<Throwable> showError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                Timber.e(e, "failed to build a collage");
            }
        };
    }

    /**
     * This is the heart of collage creation.
     * It all starts with user images, then rushes through sorting to laying out and to the final render!
     *
     * @param selectedIds a list of image ids which will be used first, can be empty, not null
     */
    private Observable<Bitmap> createCollageObservable(
            final UserInfo userInfo, final List<RectF> layout,
            final int size, @NotNull List<String> selectedIds) {

        // First, try to get from cache
        return Observable.just(userImagesCache.get(userInfo.userName()))
                // either use cached value, or proceed with fetching all image data
                .flatMap(new Func1<List<ImageInfo>, Observable<ImageInfo>>() {
                    @Override
                    public Observable<ImageInfo> call(List<ImageInfo> imageInfoList) {
                        if(imageInfoList == null) {
                            Timber.d("no cached image data for %s, retrieving from server", userInfo);
                            return createImageDataFetchObservable(userInfo);
                        } else {
                            Timber.d("using cached image data for %s", userInfo);
                            return Observable.from(imageInfoList);
                        }
                    }
                })
                // sort either by likes or by selected items
                .toSortedList(selectedIds.isEmpty() ? ImageInfo.sortByLikesDesc() : ImageInfo.sortIdsFirst(selectedIds))
                .flatMap(Functions.<ImageInfo>flatten())
                // take only amount of images we need for the collage
                .take(selectedIds.isEmpty() ? layout.size() : Math.min(selectedIds.size(), layout.size()))
                // fetch bitmap data for these image urls
                .flatMap(new Func1<ImageInfo, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(ImageInfo imageInfo) {
                        return ImageFetcher.get(CollageActivity.this, imageInfo);
                    }
                })
                .toList()
                // final move: create a collage out of them
                .map(new Func1<List<Bitmap>, Bitmap>() {
                    @Override
                    public Bitmap call(List<Bitmap> bitmaps) {
                        return CollageBuilder.create(bitmaps, layout, size, Color.WHITE);
                    }
                });
    }

    private Observable<ImageInfo> createImageDataFetchObservable(UserInfo userInfo) {
        return instagramClient.getUserImages(userInfo)
                .toList()
                .doOnNext(saveToCache(userInfo))
                .flatMap(Functions.<ImageInfo>flatten());
    }

    private Action1<List<ImageInfo>> saveToCache(final UserInfo userInfo) {
        return new Action1<List<ImageInfo>>() {
            @Override
            public void call(List<ImageInfo> imageInfoList) {
                userImagesCache.put(userInfo.userName(), imageInfoList);
            }
        };
    }

    @OnClick(R.id.button_print)
    public void onPrintRequested() {
        if(latestCollageBitmap == null) {
            // actually this is a developer error, buttons should not be shown without collage!
            Toast.makeText(this, "No collage has been built yet.", Toast.LENGTH_LONG).show();
            return;
        }
        if(!PrintHelper.systemSupportsPrint()) {
            Toast.makeText(this, getString(R.string.error_no_print_support), Toast.LENGTH_LONG).show();
            return;
        }
        Timber.d("starting print...");
        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.printBitmap("Collage photo", latestCollageBitmap);
    }

    @OnClick(R.id.button_choose)
    public void onChoosePhotosRequested() {
        Intent intent = new Intent(this, PhotoChooserActivity.class);
        intent.putExtra(Consts.EXTRA_USER_INFO, userInfo);
        startActivityForResult(intent, CHOOSE_PHOTOS_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != CHOOSE_PHOTOS_REQUEST || resultCode != RESULT_OK) return;

        List<String> chosenImageIds = data.getStringArrayListExtra(Consts.EXTRA_IMAGE_IDS);
        Timber.d("got list of chosen image ids: %s", chosenImageIds);
        createCollage(userInfo, CollageLayout.SIMPLE_2x2, DEFAULT_COLLAGE_SIZE, chosenImageIds);
    }
}
