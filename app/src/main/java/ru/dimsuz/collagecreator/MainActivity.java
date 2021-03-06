package ru.dimsuz.collagecreator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.network.InstagramClient;
import ru.dimsuz.collagecreator.util.Actions;
import rx.Observable;
import rx.android.lifecycle.LifecycleEvent;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.widget.OnTextChangeEvent;
import rx.android.widget.WidgetObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class MainActivity extends RxCompatActivity {
    @Inject
    InstagramClient instagramClient;
    @Inject
    Map<String,Typeface> typefaceCache;
    @InjectView(R.id.edit_username)
    TextView userNameView;
    @InjectView(R.id.errorView)
    TextView errorView;
    @InjectView(R.id.button_start_text)
    TextView collageButtonText;
    @InjectView(R.id.welcome_text)
    TextView welcomeTextView;
    @InjectView(R.id.progressBar)
    ProgressBar progressBar;
    @InjectView(R.id.content)
    View contentLayout;

    /**
     * Will emit notifications with all valid instagram user names entered by user.
     * Latest valid name will be cached and emitted to the subscriber immediately.
     */
    private BehaviorSubject<UserInfo> validUserInfoObservable = BehaviorSubject.create(UserInfo.INVALID_INFO);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CollageCreatorApp.get(this).inject(this);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupEditsObservable();

        // run this after setting up an observable, so that change is propagated
        if(savedInstanceState != null) {
            String name = savedInstanceState.getString("name");
            userNameView.setText(name != null ? name : "");
        }
        collageButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
        collageButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
        collageButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
        welcomeTextView.setTypeface(typefaceCache.get("Roboto Light"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", userNameView.getText().toString());
    }

    private void setupEditsObservable() {
        // While user is entering text, check the validity of the name in background, silently...
        // Only after user hits the button the validation message will be displayed to him.
        // This way there's a chance that button response will be instantaneous (either successful or not)

        // The last valid entered instagram name will be cached, so that the next subscriber
        // will get instant access to it (for example in onClick for button)
        Observable<UserInfo> observable = WidgetObservable.text(userNameView)
                // don't check too often...
                .debounce(500, TimeUnit.MILLISECONDS)
                .filter(Actions.notEmptyText())
                .flatMap(new Func1<OnTextChangeEvent, Observable<UserInfo>>() {
                    @Override
                    public Observable<UserInfo> call(OnTextChangeEvent event) {
                        final String userName = event.text().toString();
                        return createGetUserInfoObservable(userName);
                    }
                })
                // produce only valid info
                .filter(Actions.validUserInfo());

        // subscribe immediately to start watching text changes
        LifecycleObservable.bindUntilLifecycleEvent(lifecycle(), observable, LifecycleEvent.DESTROY)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<UserInfo>() {
                    @Override
                    public void call(UserInfo userInfo) {
                        errorView.setVisibility(View.GONE);
                        Timber.d("updating last valid user info: %s", userInfo);
                        validUserInfoObservable.onNext(userInfo);
                    }
                });
    }

    private Observable<UserInfo> createGetUserInfoObservable(final String userName) {
        return instagramClient.getUserInfo(userName)
                .subscribeOn(Schedulers.io())
                // do not break on error, rather return an invalid UserInfo instance
                .onErrorReturn(new Func1<Throwable, UserInfo>() {
                    @Override
                    public UserInfo call(Throwable throwable) {
                        return UserInfo.create(userName, null);
                    }
                });
    }

    @OnEditorAction(R.id.edit_username)
    boolean onEditorActionGo(TextView view, int actionId) {
        if(actionId == EditorInfo.IME_ACTION_GO) {
            // hide virtual keyboard
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            onCollageButtonClicked();
            return true;
        }
        return false;
    }

    @OnClick(R.id.button_start)
    void onCollageButtonClicked() {
        // subscribing to 'userInfoObservable' will emit a last valid user name,
        // so next step is to check whether it is the name which is currently entered:
        // if so - Bingo!
        // if not - need to check it one more time
        LifecycleObservable
                .bindUntilLifecycleEvent(lifecycle(), validUserInfoObservable, LifecycleEvent.DESTROY)
                .take(1)
                .flatMap(startFinalCheck())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(setProgressBarVisible(true))
                .finallyDo(setProgressBarVisible(false))
                .subscribe(
                        processFinalUserInfo(), // onSuccess
                        showInfoFetchError() // onError
                );
    }

    private Action1<UserInfo> processFinalUserInfo() {
        return new Action1<UserInfo>() {
            @Override
            public void call(UserInfo userInfo) {
                if(userInfo.isValid()) {
                    errorView.setVisibility(View.GONE);
                    Intent intent = new Intent(MainActivity.this, CollageActivity.class);
                    intent.putExtra(Consts.EXTRA_USER_INFO, userInfo);
                    startActivity(intent);
                } else {
                    errorView.setVisibility(View.VISIBLE);
                    String errorText = userInfo.userName().isEmpty()
                            ? getResources().getString(R.string.err_user_name_is_empty)
                            : getResources().getString(R.string.err_user_name_not_found, userInfo.userName());
                    errorView.setText(errorText);
                }
            }
        };
    }

    private Action1<Throwable> showInfoFetchError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                Timber.e(e, "failed to retrieve user info");
                errorView.setVisibility(View.VISIBLE);
                errorView.setText(R.string.err_retrieve_failed);
            }
        };
    }

    @NotNull
    private Func1<UserInfo, Observable<UserInfo>> startFinalCheck() {
        return new Func1<UserInfo, Observable<UserInfo>>() {
            @Override
            public Observable<UserInfo> call(UserInfo userInfo) {
                String userName = userNameView.getText().toString();
                boolean needsChecking = userName.isEmpty() || !userInfo.isValid() || !userName.equals(userInfo.userName());
                if(!needsChecking) {
                    // wow, no checking needed, we got it right last time, just go on!
                    Timber.d("last entered user name '%s' was correct, yay", userName);
                    return Observable.just(userInfo);
                } else {
                    Timber.d("last entered user name '%s' needs checking, running check...", userName);
                    // name changed since last validation with instagram, so run it here one more time
                    // this check can produce an error: if so, a label will be shown this time to user
                    return createGetUserInfoObservable(userName);
                }
            }
        };
    }

    private Action0 setProgressBarVisible(final boolean visible) {
        return new Action0() {
            @Override
            public void call() {
                progressBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
                        contentLayout.setVisibility(visible ? View.GONE : View.VISIBLE);
                    }
                }, visible ? 0 : 300); // delay showing content back again to prevent flickering
            }
        };
    }
}
