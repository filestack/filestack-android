package io.filepicker.adapters;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.util.ArrayList;
import java.util.HashMap;

import io.filepicker.Filepicker;
import io.filepicker.R;
import io.filepicker.models.GoogleDriveNode;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.Constants;
import io.filepicker.utils.ImageLoader;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesAdapter extends RecyclerView.Adapter<NodesAdapter.NodeViewHolder> {

    private static final int TYPE_THUMBNAIL_NAMED_IMAGE = 0;
    private static final int TYPE_THUMBNAIL_IMAGE = 1;
    private static final int TYPE_LIST_ITEM = 2;

    private boolean thumbnail = false;

    private final ArrayList<PickedFile> pickedFiles;

    private final Activity context;
    private final ArrayList<Node> nodes;

    private OnNodeClickListener nodeClickListener;
    private HashMap<GoogleDriveNode,AsyncTask<Void,Void,Void>> labelRefresher;

    public NodesAdapter(Activity context, ArrayList<Node> nodes, ArrayList<PickedFile> pickedFiles) {
        this.context = context;
        this.nodes = nodes;
        this.pickedFiles = pickedFiles;
        labelRefresher = new HashMap<>();
    }

    public ArrayList<Node> getNodes(){
        return nodes;
    }

    @Override
    public NodesAdapter.NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        NodesAdapter.NodeViewHolder viewHolder;
        View itemView;

        switch (viewType){
            case TYPE_THUMBNAIL_NAMED_IMAGE:
                    itemView = LayoutInflater.from(context).inflate(R.layout.thumbnail_item_image_named_image,parent,false);
                    viewHolder = new ThumbNameViewHolder(itemView);
                    break;

            case TYPE_THUMBNAIL_IMAGE:
                    itemView = LayoutInflater.from(context).inflate(R.layout.thumbnail_item_node, parent,false);
                    viewHolder = new ThumbImageViewHolder(itemView);
                    break;
            default:
                itemView = LayoutInflater.from(context).inflate(R.layout.list_item_node, parent,false);
                viewHolder = new ListViewHolder(itemView);

        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(NodesAdapter.NodeViewHolder holder, int position) {
        Node node = nodes.get(position);

        if(holder instanceof ThumbImageViewHolder){
            ImageLoader.getImageLoader(context)
                    .load(node.getThumbnailUrl())
                    .into(((ThumbImageViewHolder)holder).icon);
        }

        if(holder instanceof ThumbNameViewHolder){
            ((ThumbNameViewHolder) holder).name.setText(node.displayName);
            ((ThumbNameViewHolder) holder).icon.setImageResource(node.getImageResource());
            if(node instanceof GoogleDriveNode){
                GoogleDriveNode gNode = (GoogleDriveNode)node;
                if(gNode.driveId.equals(gNode.displayName)){
                    ((ThumbNameViewHolder) holder).name.setText("Fetching Email..");
                    refreshMail(gNode);
                }
            }
        }

        if(holder instanceof  ListViewHolder){
            ((ListViewHolder) holder).name.setText(node.displayName);
            if (!node.isDir && node.hasThumbnail()) {
                ImageLoader.getImageLoader(context).load(node.getThumbnailUrl()).into(((ListViewHolder) holder).icon);
            } else {
                ((ListViewHolder) holder).icon.setImageResource(node.getImageResource());
            }

            if(node instanceof GoogleDriveNode){
                GoogleDriveNode gNode = (GoogleDriveNode)node;
                if(gNode.driveId.equals(gNode.displayName)){
                    ((ListViewHolder) holder).name.setText("Fetching Email..");
                    refreshMail(gNode);
                }
            }
        }

        if (PickedFile.containsNode(pickedFiles, node)) {
            holder.holder.setAlpha(Constants.ALPHA_FADED);
        } else {
            holder.holder.setAlpha(Constants.ALPHA_NORMAL);
        }

        holder.holder.setTag(node);
        holder.holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Node nod = (Node)v.getTag();
                getNodeClickListener().onNodeClick(nod);
            }
        });

    }

    @Override
    public int getItemCount() {
        return nodes.size();
    }

    @Override
    public int getItemViewType(int position) {
        Node node =  nodes.get(position);
        if (thumbnail) {
            // Directories and files with names (not images)
            if (node.isDir || !node.isImage()) {
                return TYPE_THUMBNAIL_NAMED_IMAGE;
            } else {
                return TYPE_THUMBNAIL_IMAGE;
            }
        } else {
            return TYPE_LIST_ITEM;
        }
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    public OnNodeClickListener getNodeClickListener() {
        return nodeClickListener;
    }

    private void refreshMail(GoogleDriveNode node){
        if(labelRefresher.get(node) == null){
            labelRefresher.put(node, (new AsyncTask<Void, Void, Void>() {
                private Gmail mService = null;
                private GoogleDriveNode node;
                public AsyncTask<Void, Void, Void> setNode(GoogleDriveNode node){
                    this.node = node;
                    return this;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    HttpTransport transport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                    mService = new com.google.api.services.gmail.Gmail.Builder(
                            transport, jsonFactory, Filepicker.getGmailCredential(context))
                            .setApplicationName("FileStack")
                            .build();

                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(nodes.indexOf(node));
                        }
                    });
                    labelRefresher.remove(node);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Message em = mService.users().messages().get("me", node.driveId).setFields("payload,snippet").execute();
                        node.displayName = "";
                        for(MessagePartHeader head:em.getPayload().getHeaders()){
                            if(head.get("name").equals("Subject")){
                                node.displayName += head.get("value");
                            }
                        }

                        if(node.displayName.length() <= 0){
                            node.displayName += em.getSnippet();
                        }

                        if(node.displayName.length() <= 0) {
                            for (MessagePartHeader head : em.getPayload().getHeaders()) {
                                if (head.get("name").equals("From")) {
                                    node.displayName += head.get("name") + ":" + head.get("value");
                                }
                            }
                        }
                    }catch (Exception e) {
                        node.displayName = "Mail text not found.";
                    }
                    return null;
                }
            }).setNode(node));
            labelRefresher.get(node).execute();
        }
    }

    public void setNodeClickListener(OnNodeClickListener nodeClickListener) {
        this.nodeClickListener = nodeClickListener;
    }

    public interface OnNodeClickListener{
        void onNodeClick(Node node);
    }


    private static class ListViewHolder extends NodeViewHolder{
        TextView name;
        ImageView icon;
        ListViewHolder(View view){
                super(view);
                icon = (ImageView) view.findViewById(R.id.imageNode);
                name = (TextView) view.findViewById(R.id.tvNodeName);
        }
    }


    private static class ThumbImageViewHolder extends NodeViewHolder{
        ImageView icon;
        ThumbImageViewHolder(View view){
                super(view);
                icon = (ImageView) view.findViewById(R.id.thumbNode);
        }
    }

    private static class ThumbNameViewHolder extends NodeViewHolder {
        TextView name;
        ImageView icon;
        ThumbNameViewHolder(View view){
                super(view);
                icon = (ImageView) view.findViewById(R.id.imageFolder);
                name = (TextView) view.findViewById(R.id.tvFolderName);
        }
    }

    public static class NodeViewHolder extends RecyclerView.ViewHolder{
        View holder;

        NodeViewHolder(View view){
            super(view);
            holder = view.findViewById(R.id.holder);
        }

    }
}
