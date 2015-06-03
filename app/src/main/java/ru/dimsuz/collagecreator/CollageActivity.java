package ru.dimsuz.collagecreator;

import android.os.Bundle;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.network.InstagramClient;
import rx.android.lifecycle.LifecycleEvent;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Presents a user with a collage and options to adjust it
 */
public class CollageActivity extends RxCompatActivity {
    @Inject
    InstagramClient instagramClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collage);

        CollageCreatorApp.get(this).inject(this);
        ButterKnife.inject(this);

        UserInfo userInfo = getIntent().getParcelableExtra(Consts.EXTRA_USER_INFO);
        if(userInfo == null || !userInfo.isValid()) {
            throw new RuntimeException("required user info is missing");
        }
        LifecycleObservable.bindUntilLifecycleEvent(lifecycle(),
                instagramClient.getUserImages(userInfo), LifecycleEvent.DESTROY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<List<ImageInfo>>() {
                            @Override
                            public void call(List<ImageInfo> imageInfoList) {
                                Timber.d("got image info list of size %d: %s", imageInfoList.size(), imageInfoList);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable e) {
                                Timber.e(e, "failed to obtain image info");
                            }
                        });
    }
}
