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
import io.filepicker.utils.ImageLoader;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesAdapter<T> extends ArrayAdapter<T> {

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

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Node node = (Node) nodes.get(position);
        ViewHolder viewHolder;

        if(convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();

            convertView = inflater.inflate(getLayoutId(node), null);

            viewHolder = new ViewHolder();

            if (thumbnail) {
                if (node.isDir) {
                    viewHolder.icon = (ImageView) convertView.findViewById(R.id.imageFolder);
                    viewHolder.name = (TextView) convertView.findViewById(R.id.tvFolderName);
                } else {
                    viewHolder.icon = (ImageView) convertView.findViewById(R.id.thumbNode);
                }
            } else {
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.imageNode);
                viewHolder.name = (TextView) convertView.findViewById(R.id.tvNodeName);
            }

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // GridView
        if(thumbnail) {
            if (node.isDir) {
                viewHolder.name.setText(node.displayName);
                viewHolder.icon.setImageResource(node.getImageResource());

            } else {
                ImageLoader.getImageLoader(context).load(node.getThumbnailUrl()).into(viewHolder.icon);
            }

        // ListView
        } else {
            if(!node.isDir && node.hasThumbnail()) {
                ImageLoader.getImageLoader(context).load(node.getThumbnailUrl()).into(viewHolder.icon);
            } else {
                viewHolder.icon.setImageResource(node.getImageResource());
            }
            viewHolder.name.setText(node.displayName);
        }

        if(PickedFile.containsPosition(pickedFiles, position)) {
            convertView.setAlpha(0.4f);
        } else {
            convertView.setAlpha(1);
        }

        return convertView;
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    private int getLayoutId(Node node) {
        int layoutId;
        if (thumbnail) {
            if(node.isDir) {
                layoutId = R.layout.thumbnail_item_folder;
            } else {
                layoutId = R.layout.thumbnail_item_node;
            }
        } else {
            layoutId = R.layout.list_item_node;
        }

        return layoutId;
    }
}
