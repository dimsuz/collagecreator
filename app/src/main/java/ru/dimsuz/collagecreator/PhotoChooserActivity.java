package ru.dimsuz.collagecreator;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import timber.log.Timber;

/**
 * An activity to choose photos that make up collage
 */
public class PhotoChooserActivity extends RxCompatActivity {
    // using a list view, because RecyclerView doesn't support multiple choice action modes so far
    // would take time to adapt that
    @InjectView(R.id.listView)
    ListView listView;
    @InjectView(R.id.button_start_text)
    TextView collageButtonText;

    @Inject
    LruCache<String,List<ImageInfo>> userImagesCache;
    @Inject
    Map<String,Typeface> typefaceCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photos);

        CollageCreatorApp.get(this).inject(this);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final UserInfo userInfo = getIntent().getParcelableExtra(Consts.EXTRA_USER_INFO);
        if(userInfo == null || !userInfo.isValid()) {
            throw new RuntimeException("required user info is missing");
        }
        List<ImageInfo> userImages = userImagesCache.get(userInfo.userName());
        if(userImages == null) {
            // if this happens... well... this should not! cache is filled,
            // right before button to invoke this activity is available to user
            Timber.e("user images were not retrieved, can not continue!");
            finish();
        }

        setupListView(userImages);
        collageButtonText.setTypeface(typefaceCache.get("Roboto Medium"));
    }

    public void setupListView(List<ImageInfo> imageInfoList) {
        ImageListAdapter adapter = new ImageListAdapter(typefaceCache);
        adapter.swapData(imageInfoList);
        listView.setAdapter(adapter);
    }
}
