package io.filepicker.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import io.filepicker.R;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.Constants;
import io.filepicker.utils.ImageLoader;
import io.filepicker.utils.Utils;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesAdapter<T> extends ArrayAdapter<T> {

    private static final int VIEW_TYPE_COUNT = 3;

    private static final int TYPE_THUMUBNAIL_NAMED_IMAGE = 0;
    private static final int TYPE_THUMUBNAIL_IMAGE = 1;
    private static final int TYPE_LIST_ITEM         = 2;

    private boolean thumbnail = false;

    private ArrayList<PickedFile> pickedFiles;

    private Activity context;
    private ArrayList<T> nodes;

    public NodesAdapter(Activity context, ArrayList<T> nodes, ArrayList<PickedFile> pickedFiles) {
        super(context, R.layout.list_item_node, nodes);
        this.context = context;
        this.nodes = nodes;
        this.pickedFiles = pickedFiles;
    }

    static class ViewHolder {
        TextView name;
        ImageView icon;

        void setName(String value) {
            if(name != null) {
                this.name.setText(value);
            }
        }

        void setIcon(int res) {
            if(icon != null) {
                icon.setImageResource(res);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder viewHolder;

        if(row == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = context.getLayoutInflater();

            switch (getItemViewType(position)) {
                case TYPE_THUMUBNAIL_NAMED_IMAGE:
                    row = inflater.inflate(R.layout.thumbnail_item_image_named_image, null);
                    viewHolder.icon = (ImageView) row.findViewById(R.id.imageFolder);
                    viewHolder.name = (TextView) row.findViewById(R.id.tvFolderName);
                    break;
                case TYPE_THUMUBNAIL_IMAGE:
                    row = inflater.inflate(R.layout.thumbnail_item_node, null);
                    viewHolder.icon = (ImageView) row.findViewById(R.id.thumbNode);
                    break;
                default:
                    row = inflater.inflate(R.layout.list_item_node, null);
                    viewHolder.icon = (ImageView) row.findViewById(R.id.thumbNode);
                    viewHolder.icon = (ImageView) row.findViewById(R.id.imageNode);
                    viewHolder.name = (TextView) row.findViewById(R.id.tvNodeName);
            }

            row.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) row.getTag();
        }

        Node node = (Node) nodes.get(position);

        switch (getItemViewType(position)) {
            case TYPE_THUMUBNAIL_NAMED_IMAGE:
                viewHolder.setName(Utils.getShortName(node.displayName));
                viewHolder.setIcon(node.getImageResource());
                break;
            case TYPE_THUMUBNAIL_IMAGE:
                ImageLoader.getImageLoader(context)
                        .load(node.getThumbnailUrl())
                        .into(viewHolder.icon);
                break;
            default:
                viewHolder.setName(node.displayName);
                if(!node.isDir && node.hasThumbnail()) {
                    ImageLoader.getImageLoader(context).load(node.getThumbnailUrl()).into(viewHolder.icon);
                } else {
                    viewHolder.setIcon(node.getImageResource());
                }
        }

        if (PickedFile.containsPosition(pickedFiles, position)) {
            row.setAlpha(Constants.ALPHA_FADED);
        } else {
            row.setAlpha(Constants.ALPHA_NORMAL);
        }

        return row;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        Node node = (Node) nodes.get(position);

        if(thumbnail) {
            // Directories and files with names (not images)
            if(node.isDir || !node.isImage()) {
                return TYPE_THUMUBNAIL_NAMED_IMAGE;
            }
            else {
                return TYPE_THUMUBNAIL_IMAGE;
            }
        } else {
            return TYPE_LIST_ITEM;
        }
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }
}
