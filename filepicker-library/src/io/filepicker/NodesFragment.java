package io.filepicker;

import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
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
    ArrayList<PickedFile> pickedFiles = new ArrayList<PickedFile>();

    AbsListView currentView;
    ProgressBar mProgressBar;
    Button mUploadFilesButton;
    NodesAdapter<Node> nodesAdapter;

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

        nodesAdapter = new NodesAdapter(getActivity(), nodes, pickedFiles);

        if(viewType.equals(THUMBNAILS_VIEW))
            nodesAdapter.setThumbnail(true);

        currentView.setAdapter(nodesAdapter);

        currentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Clicked node object
                Node node = (Node) parent.getAdapter().getItem(position);

                // If node is dir then open it
                if (node.isDir()) {
                    openDir(node);
                } else {
                    // Proceed single file
                    PickedFile pickedFile = new PickedFile(node, position);

                    if(canPickMultiple()) {
                        if (PickedFile.containsPosition(pickedFiles, pickedFile.getPosition())){
                            PickedFile.removeAtPosition(pickedFiles, position);
                            view.setAlpha(1);

                        } else {
                            pickedFiles.add(pickedFile);
                            view.setAlpha(0.2f);

                        }


                        setUploadButton();



                    } else {
                        showProgressBar();
                        pickedFiles.add(pickedFile);
                        getContract().pickFiles(PickedFile.getNodes(pickedFiles));
                    }
                }
            }
        });

        currentView.setVisibility(View.VISIBLE);

        mUploadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pickedFiles.size() > 0) {
                    showProgressBar();
                    getContract().pickFiles(PickedFile.getNodes(pickedFiles));
                }
            }
        });
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void setUploadButton() {
        if (pickedFiles.size() > 0) {
            mUploadFilesButton.setVisibility(View.VISIBLE);

            String btnText;
            if(pickedFiles.size() == 1) {
                btnText = "Upload 1 file";
            } else {
                btnText = "Upload " + pickedFiles.size() + " files";
            }

            mUploadFilesButton.setText(btnText);
        } else {
            mUploadFilesButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        pickedFiles.clear();
        mUploadFilesButton.setVisibility(View.GONE);

        super.onPause();
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

    private void openDir(Node node) {
        if(node.isGallery()){
            getContract().openGallery();
        } else if (node.isCamera()) {
            getContract().openCamera();
        } else {
            getContract().showContent(node);
        }
    }

    private void proceedFile(PickedFile file) {
        // Check if node exists. If so then remove it. If not then add it.
        if(pickedFiles.contains(file)) {
            pickedFiles.remove(file);
        } else {
            pickedFiles.add(file);
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
            getContract().pickFiles(PickedFile.getNodes(pickedFiles));
        }
    }

    private void highlightNode(View view) {
        if (view.getAlpha() == 1) {
            view.setAlpha(0.2f);
        } else {
            view.setAlpha(1);
        }
    }

    private boolean canPickMultiple() {
        return PreferencesUtils.newInstance(getActivity()).getMultiple();
    }

    public Contract getContract() {
        return (Contract) getActivity();
    }

}