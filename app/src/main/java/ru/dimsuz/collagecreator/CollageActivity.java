package ru.dimsuz.collagecreator;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Presents a user with a collage and options to adjust it
 */
public class CollageActivity extends RxCompatActivity {
    @Inject
    InstagramClient instagramClient;
    @InjectView(R.id.collageView)
    ImageView collageView;
    @InjectView(R.id.progressBar)
    View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collage);

        CollageCreatorApp.get(this).inject(this);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        UserInfo userInfo = getIntent().getParcelableExtra(Consts.EXTRA_USER_INFO);
        if(userInfo == null || !userInfo.isValid()) {
            throw new RuntimeException("required user info is missing");
        }

        createCollage(userInfo, CollageLayout.SIMPLE_2x2);
    }

    private void createCollage(UserInfo userInfo, final List<RectF> layout) {
        Observable<Bitmap> collageObservable = createCollageObservable(userInfo, layout);
        LifecycleObservable.bindUntilLifecycleEvent(lifecycle(), collageObservable, LifecycleEvent.DESTROY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(showCollage(), showError());
    }

    @NotNull
    private Action1<Bitmap> showCollage() {
        return new Action1<Bitmap>() {
            @Override
            public void call(Bitmap collageBitmap) {
                Timber.d("got collage made: %dx%d", collageBitmap.getWidth(), collageBitmap.getHeight());
                collageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                collageView.setImageBitmap(collageBitmap);
                progressBar.setVisibility(View.GONE);
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

    private Observable<Bitmap> createCollageObservable(UserInfo userInfo, final List<RectF> layout) {
        return instagramClient.getUserImages(userInfo)
                .toSortedList(ImageInfo.sortByLikesDesc())
                .flatMap(Functions.<ImageInfo>flatten())
                .take(layout.size())
                .flatMap(new Func1<ImageInfo, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(ImageInfo imageInfo) {
                        return ImageFetcher.get(CollageActivity.this, imageInfo);
                    }
                })
                .toList()
                .map(new Func1<List<Bitmap>, Bitmap>() {
                    @Override
                    public Bitmap call(List<Bitmap> bitmaps) {
                        return CollageBuilder.create(bitmaps, layout, 300, Color.WHITE);
                    }
                });
    }

}
