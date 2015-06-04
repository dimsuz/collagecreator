package ru.dimsuz.collagecreator;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.views.CheckableImageView;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * An activity to choose photos that make up collage
 */
public class PhotoChooserActivity extends RxCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
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
    @Nullable
    private ActionMode actionMode;
    private List<ImageInfo> userImages;

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
        // due to inconsistencies of list view choice mode, this needs to partially be moved on
        // an adapter's shoulders. (main reason is the lack of state_activated prior to API11 and
        // all weirdness which was fixed only later)
        adapter.setCheckedStateProvider(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer position) {
                return listView.isItemChecked(position);
            }
        });
        listView.setAdapter(adapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        startActionMode();
    }

    private void startActionMode() {
        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(getResources().getString(R.string.x_out_of_y,
                        listView.getCheckedItemIds().length, userImages.size()));
                actionMode = mode;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                finish();
            }
        });
    }

    @Override
    public void onItemClick(@NotNull AdapterView<?> adapterView, @NotNull View item, int position, long id) {
        // set in place to avoid reloading all items in adapter. But adapter will be still aware of
        // this and do good when using recycled views
        boolean checked = listView.isItemChecked(position);
        CheckableImageView imageView = (CheckableImageView) item.findViewById(R.id.image);
        imageView.setChecked(checked);
        if(actionMode != null) {
            actionMode.setTitle(getResources().getString(R.string.x_out_of_y,
                    listView.getCheckedItemPositions().size(), userImages.size()));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long ld) {
        if(actionMode == null) {
            startActionMode();
            return true;
        }
        return false;
    }
}
