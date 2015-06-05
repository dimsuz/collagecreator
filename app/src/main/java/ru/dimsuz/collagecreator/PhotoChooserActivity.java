package ru.dimsuz.collagecreator;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.views.CheckableImageView;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * An activity to choose photos that make up collage
 */
public class PhotoChooserActivity extends RxCompatActivity implements AdapterView.OnItemClickListener {
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
    private ArrayList<Integer> selectionOrder = new ArrayList<>();
    private int collageLayoutSize;

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
        userImages = userImagesCache.get(userInfo.userName());
        if(userImages == null) {
            // if this happens... well... this should not! cache is filled,
            // right before button to invoke this activity is available to user
            Timber.e("user images were not retrieved, can not continue!");
            finish();
        }
        collageLayoutSize = getIntent().getIntExtra(Consts.EXTRA_COLLAGE_LAYOUT_SIZE, 0);

        // there's no guaranteed about sorting order, so must sort
        Collections.sort(userImages, ImageInfo.comparatorByLikes());
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
        startActionMode();
    }

    private void startActionMode() {
        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                updateTitle(mode);
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

    private void updateTitle(@Nullable ActionMode mode) {
        if(mode != null) {
            int count = listView.getCheckedItemIds().length;
            String title = getResources().getString(R.string.x_out_of_y, count, userImages.size());
            if(collageLayoutSize - count > 0) {
                title += " " + getResources().getString(R.string.more_hint, collageLayoutSize - count);
            }
            mode.setTitle(title);
        }
    }

    @Override
    public void onItemClick(@NotNull AdapterView<?> adapterView, @NotNull View item, int position, long id) {
        // set in place to avoid reloading all items in adapter. But adapter will be still aware of
        // this and do good when using recycled views
        boolean checked = listView.isItemChecked(position);
        CheckableImageView imageView = (CheckableImageView) item.findViewById(R.id.image);
        imageView.setChecked(checked);
        updateTitle(actionMode);
        if(checked) {
            selectionOrder.add(position);
        } else {
            selectionOrder.remove((Integer)position);
        }
    }

    @OnClick(R.id.button_start)
    public void onChooseFinished() {
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        ImageListAdapter adapter = (ImageListAdapter) listView.getAdapter();
        ArrayList<String> imageIds = getSelectedIds(adapter.getData(), checkedItemPositions, selectionOrder);
        if(imageIds.isEmpty()) {
            Toast.makeText(this, R.string.err_nothing_selected, Toast.LENGTH_LONG).show();
            return;
        }
        Intent result = new Intent();
        result.putStringArrayListExtra(Consts.EXTRA_IMAGE_IDS, imageIds);
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * @return selected image ids, sorted by order in which they were selected
     */
    private static ArrayList<String> getSelectedIds(final List<ImageInfo> data, SparseBooleanArray itemPositions,
                                                    final List<Integer> selectionOrder) {
        final ArrayList<ImageInfo> selectedItems = new ArrayList<>(itemPositions.size());
        for(int i=0, sz=data.size(); i<sz; i++) {
            if(itemPositions.get(i)) {
                selectedItems.add(data.get(i));
            }
        }
        // Sort it so that items will come in the order in which user selected them
        Collections.sort(selectedItems, new Comparator<ImageInfo>() {
            @Override
            public int compare(ImageInfo i1, ImageInfo i2) {
                int pos1 = data.indexOf(i1);
                int pos2 = data.indexOf(i2);
                // selectionOrder contains a list of positions in their selection order,
                // so positions at the end of selectionOrder were ones selected last
                return ImageInfo.compareInts(selectionOrder.indexOf(pos1), selectionOrder.indexOf(pos2));
            }
        });

        final ArrayList<String> selectedIds = new ArrayList<>(itemPositions.size());
        for(int i=0, size=selectedItems.size(); i<size; i++) {
            selectedIds.add(selectedItems.get(i).id());
        }
        return selectedIds;
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Integer> order = savedInstanceState.getIntegerArrayList("selectionOrder");
        selectionOrder = order == null ? new ArrayList<Integer>() : order;
        if(actionMode != null) {
            updateTitle(actionMode);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList("selectionOrder", selectionOrder);
    }
}
