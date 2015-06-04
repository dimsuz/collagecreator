package ru.dimsuz.collagecreator;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.views.CheckableImageView;
import rx.functions.Func1;

/**
 * Adapter with image info list items
 */
public class ImageListAdapter extends BaseAdapter {
    private final Map<String, Typeface> fontCache;
    private List<ImageInfo> data = Collections.emptyList();
    private Time time = new Time();
    private Func1<Integer, Boolean> checkStateProvider;

    public ImageListAdapter(Map<String, Typeface> fontCache) {
        this.fontCache = fontCache;
    }

    public void swapData(List<ImageInfo> imageInfoList) {
        this.data = new ArrayList<>(imageInfoList);
    }

    @NotNull
    public List<ImageInfo> getData() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return data.get(i).id().hashCode();
    }

    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup viewGroup) {
        View view;
        if(convertView == null) {
            view = createView(viewGroup);
        } else {
            view = convertView;
        }
        bindView(position, (ViewHolder) view.getTag());
        return view;
    }

    private View createView(ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_info_item, viewGroup, false);
        ViewHolder holder = new ViewHolder();
        ButterKnife.inject(holder, view);
        holder.imageView.setForeground(viewGroup.getResources().getDrawable(R.drawable.check_icon));
        holder.likesView.setTypeface(fontCache.get("Roboto Regular"));
        holder.dateView.setTypeface(fontCache.get("Roboto Regular"));
        holder.titleView.setTypeface(fontCache.get("Roboto Light"));
        view.setTag(holder);
        return view;
    }

    private void bindView(int position, ViewHolder holder) {
        ImageInfo info = data.get(position);
        Context context = holder.imageView.getContext();
        //
        // Image
        //
        int imageSize = context.getResources().getDimensionPixelSize(R.dimen.chooser_image_size);
        Picasso.with(context)
                .load(info.url())
                .placeholder(R.drawable.placeholder)
                .resize(imageSize, imageSize)
                .centerInside()
                .into(holder.imageView);
        if(checkStateProvider != null) {
            holder.imageView.setChecked(checkStateProvider.call(position));
        }

        //
        // Date
        //
        time.set(info.timestamp());
        holder.dateView.setText(time.format("%d %b %Y"));

        //
        // Title
        //
        holder.titleView.setText(formatTitle(context, info.title()));

        //
        // Likes
        //
        holder.likesView.setText(String.valueOf(info.likesCount()));
    }

    private static CharSequence formatTitle(Context context, String title) {
        CharSequence result;
        if(title == null || title.isEmpty()) {
            String s = context.getString(R.string.no_title_text);
            SpannableStringBuilder sb = new SpannableStringBuilder(s);
            sb.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.lightGray)),
                    0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            result = sb;
        } else {
            result = title;
        }
        return result;
    }

    /**
     * If title is too long, truncates it
     */
    private static CharSequence limitTitle(CharSequence title) {
        int maxLength = 12;
        return title.length() > maxLength ? title.subSequence(0, maxLength-3) + "..." : title;
    }

    public void setCheckedStateProvider(Func1<Integer, Boolean> checkStateProvider) {
        this.checkStateProvider = checkStateProvider;
    }

    static class ViewHolder {
        @InjectView(R.id.image)
        CheckableImageView imageView;
        @InjectView(R.id.date)
        TextView dateView;
        @InjectView(R.id.title)
        TextView titleView;
        @InjectView(R.id.likes)
        TextView likesView;
    }
}
