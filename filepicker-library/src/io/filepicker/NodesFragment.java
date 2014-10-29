package io.filepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;

import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.Node;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.Utils;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesFragment extends Fragment {

    // Activity needs to implement these methods
    public interface Contract {
        public void openCamera();
        public void openGallery();
        public void pickFiles(ArrayList<Node> node);
        public void showContent(Node node);
        public void logoutUser(Node node);
    }

    static final String KEY_NODES = "nodes";
    static final String KEY_PARENT_NODE = "parent_node";
    static final String KEY_VIEW_TYPE = "viewType";

    public static final String LIST_VIEW   = "list";
    public static final String THUMBNAILS_VIEW  = "thumbnails";

    String viewType;
    ArrayList<Node> nodes;
    Node parentNode;

    // Used when user can pick many files at once
    ArrayList<Node> pickedFiles = new ArrayList<Node>();

    AbsListView currentView;
    ProgressBar mProgressBar;
    Button mUploadFilesButton;

    public static NodesFragment newInstance(Node parentNode, Node[] nodes, String viewType) {
        NodesFragment frag = new NodesFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_PARENT_NODE, parentNode);
        args.putParcelableArray(KEY_NODES, nodes);
        args.putString(KEY_VIEW_TYPE, viewType);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle == null)
            getActivity().finish();

        viewType = bundle.getString(KEY_VIEW_TYPE);

        Parcelable[] parcels = bundle.getParcelableArray(KEY_NODES);
        nodes = new ArrayList<Node>();
        for(Parcelable parcel : parcels) {
            nodes.add((Node) parcel);
        }
        parentNode = bundle.getParcelable(KEY_PARENT_NODE);

        if(parentNode != null && Utils.isProvider(parentNode)){
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nodes, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBarNode);
        mUploadFilesButton = (Button) view.findViewById(R.id.btnUploadFiles);

        if(viewType.equals(LIST_VIEW)) {
            currentView = (ListView) view.findViewById(R.id.listView);
        } else if(viewType.equals(THUMBNAILS_VIEW)) {
            currentView = (GridView) view.findViewById(R.id.gridView);
        } else {
            showEmptyView(view);
        }

        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(currentView == null)
            return;

        NodesAdapter<Node> nodesAdapter =
                new NodesAdapter(getActivity(), nodes);

        if(viewType.equals(THUMBNAILS_VIEW))
            nodesAdapter.setThumbnail(true);

        currentView.setAdapter(nodesAdapter);

        currentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setEnabled(false);

                Node node = (Node) parent.getAdapter().getItem(position);

                if(node.isDir()) {
                    onFolderClicked(node);
                } else {
                    toggleHighlight(view);
                    onFileClicked(node);
                }
            }
        });

        currentView.setVisibility(View.VISIBLE);

        mUploadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pickedFiles.size() > 0) {
                    showProgressBar();
                    getContract().pickFiles(pickedFiles);
                }
            }
        });
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filepicker, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_logout) {
            getContract().logoutUser(parentNode);
        }

        return super.onOptionsItemSelected(item);
    }

    private void showEmptyView(View view) {
        view.findViewById(R.id.emptylistView).setVisibility(View.VISIBLE);
    }

    private void onFolderClicked(Node node) {
        if(node.isGallery()){
            getContract().openGallery();
        } else if (node.isCamera()) {
            getContract().openCamera();
        } else {
            getContract().showContent(node);
        }
    }

    private void onFileClicked(Node node) {
        // Check if node exists. If so then remove it. If not then add it.
        if(pickedFiles.contains(node)) {
            pickedFiles.remove(node);
        } else {
            pickedFiles.add(node);
        }

        // Show/hide upload files button
        if(canPickMultiple()) {
            if(pickedFiles.size() > 0) {
                mUploadFilesButton.setVisibility(View.VISIBLE);
            } else {
                mUploadFilesButton.setVisibility(View.GONE);
            }
        } else {
            showProgressBar();
            getContract().pickFiles(pickedFiles);
        }
    }

    private boolean canPickMultiple() {
        return PreferencesUtils.newInstance(getActivity()).getMultiple();
    }

    private void toggleHighlight(View view) {
        if(view.getAlpha() == 1) {
            view.setAlpha(0.2f);
        } else {
            view.setAlpha(1);
        }
    }

    public Contract getContract() {
        return (Contract) getActivity();
    }
}