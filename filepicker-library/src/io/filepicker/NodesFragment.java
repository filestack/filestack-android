package io.filepicker;

import android.os.Bundle;
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
import io.filepicker.utils.Constants;
import io.filepicker.utils.PreferencesUtils;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesFragment extends Fragment {

    // Activity needs to implement these methods
    public interface Contract {
        public void openCamera();
        public void openGallery();
        public void pickFiles(ArrayList<Node> node);
        public void showNextNode(Node node);
        public void logoutUser();
    }

    private static final String KEY_NODES = "nodes";
    private  static final String KEY_PARENT_NODE = "parent_node";
    private static final String KEY_VIEW_TYPE = "viewType";

    private static final String PARENT_NODE_STATE = "parent_node_state";
    private static final String NODES_STATE = "nodes_state";
    private static final String VIEW_TYPE_STATE = "view_type_state";

    private String viewType;
    private ArrayList<Node> nodes;
    private Node parentNode;

    // Used when user can pick many files at once
    private ArrayList<PickedFile> pickedFiles = new ArrayList<>();

    private AbsListView currentView;
    private ProgressBar mProgressBar;
    private Button mUploadFilesButton;
    private NodesAdapter<Node> nodesAdapter;

    public static NodesFragment newInstance(Node parentNode, ArrayList<Node> nodes, String viewType) {
        NodesFragment frag = new NodesFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_PARENT_NODE, parentNode);
        args.putParcelableArrayList(KEY_NODES, nodes);
        args.putString(KEY_VIEW_TYPE, viewType);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            viewType = savedInstanceState.getString(VIEW_TYPE_STATE);
            nodes = savedInstanceState.getParcelableArrayList(NODES_STATE);
            parentNode = savedInstanceState.getParcelable(PARENT_NODE_STATE);
        } else {
            Bundle bundle = getArguments();

            if (bundle == null) {
                getActivity().finish();
            } else {
                viewType = bundle.getString(KEY_VIEW_TYPE);
                nodes = bundle.getParcelableArrayList(KEY_NODES);
                parentNode = bundle.getParcelable(KEY_PARENT_NODE);
            }
        }

        if(nodes == null) {
            nodes = new ArrayList<>();
        }
        if(parentNode == null){
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nodes, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBarNode);
        mUploadFilesButton = (Button) view.findViewById(R.id.btnUploadFiles);

        if(viewType.equals(Constants.LIST_VIEW)) {
            currentView = (ListView) view.findViewById(R.id.listView);
        } else if(viewType.equals(Constants.THUMBNAILS_VIEW)) {
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

        if(viewType.equals(Constants.THUMBNAILS_VIEW))
            nodesAdapter.setThumbnail(true);

        currentView.setAdapter(nodesAdapter);

        currentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Clicked node object
                Node node = (Node) parent.getAdapter().getItem(position);

                // If node is dir then open it
                if (node.isDir) {
                    openDir(node);
                } else {
                    // Proceed single file
                    PickedFile pickedFile = new PickedFile(node, position);

                    if(canPickMultiple()) {
                        updatePickedList(view, pickedFile);
                        setUploadButton();
                    } else {
                        view.setAlpha(Constants.ALPHA_FADED);
                        uploadSingleFile(pickedFile);
                    }
                }
            }
        });

        currentView.setVisibility(View.VISIBLE);

        mUploadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pickedFiles.size() > 0) {
                    showProgress();
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
            getContract().logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(VIEW_TYPE_STATE, viewType);
        outState.putParcelableArrayList(NODES_STATE, nodes);
        outState.putParcelable(PARENT_NODE_STATE, parentNode);

        super.onSaveInstanceState(outState);
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
            getContract().showNextNode(node);
        }
    }

    private void uploadSingleFile(PickedFile pickedFile) {
        showProgress();
        pickedFiles.add(pickedFile);
        getContract().pickFiles(PickedFile.getNodes(pickedFiles));
    }

    private void showProgress() {
        showProgressBar();
        currentView.setEnabled(false);
    }

    private void updatePickedList(View view, PickedFile pickedFile) {
        if (PickedFile.containsPosition(pickedFiles, pickedFile.position)){
            PickedFile.removeAtPosition(pickedFiles, pickedFile.position);
            view.setAlpha(1);
        } else {
            pickedFiles.add(pickedFile);
            view.setAlpha(Constants.ALPHA_FADED);
        }
    }

    private boolean canPickMultiple() {
        return PreferencesUtils.newInstance(getActivity()).getMultiple();
    }

    Contract getContract() {
        return (Contract) getActivity();
    }

}