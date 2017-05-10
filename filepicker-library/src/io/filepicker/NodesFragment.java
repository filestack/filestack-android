package io.filepicker;

import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.GoogleDriveNode;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.Constants;
import io.filepicker.utils.EndlessRecyclerViewScrollListener;
import io.filepicker.utils.LoadMoreEvent;
import io.filepicker.utils.PreferencesUtils;

/**
 * Created by maciejwitowski on 10/22/14.
 */
public class NodesFragment extends Fragment {

    private static final String KEY_NODES = "nodes";
    private static final String KEY_PARENT_NODE = "parent_node";
    private static final String KEY_VIEW_TYPE = "viewType";

    private static final String PARENT_NODE_STATE = "parent_node_state";
    private static final String NODES_STATE = "nodes_state";
    private static final String VIEW_TYPE_STATE = "view_type_state";

    private String viewType;
    private ArrayList<Node> nodes;
    private Node parentNode;

    private NodesAdapter nodesAdapter;

    // Used when user can pick many files at once
    private final ArrayList<PickedFile> pickedFiles = new ArrayList<>();

    //private  currentView;
    private RecyclerView recyclerView;
    private ProgressBar mProgressBar;
    private Button mUploadFilesButton;

    public static NodesFragment newInstance(Node parentNode, ArrayList<? extends Node> nodes, String viewType) {
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

        if (savedInstanceState != null) {
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

        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        if (parentNode == null) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nodes, container, false);
        RecyclerView.LayoutManager layoutManager = null;

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBarNode);
        mUploadFilesButton = (Button) view.findViewById(R.id.btnUploadFiles);

        recyclerView = (RecyclerView)view.findViewById(R.id.recycler_list);
        recyclerView.setHasFixedSize(true);


        switch (viewType) {
            case Constants.LIST_VIEW:
                recyclerView .setLayoutManager(layoutManager = new LinearLayoutManager(getActivity()));

                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                        ((LinearLayoutManager)recyclerView.getLayoutManager()).getOrientation());
                recyclerView.addItemDecoration(dividerItemDecoration);
                break;
            case Constants.THUMBNAILS_VIEW:
                recyclerView.setLayoutManager(layoutManager = new GridLayoutManager(getActivity(),3,GridLayoutManager.VERTICAL,false));
                break;
            default:
                showEmptyView(view);
                break;
        }

        if(layoutManager != null) {
            if(layoutManager instanceof LinearLayoutManager) {
                recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((LinearLayoutManager) layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                        EventBus.getDefault().post(new LoadMoreEvent());
                    }
                });
            }

            if(layoutManager instanceof  GridLayoutManager){
                recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((GridLayoutManager) layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                        EventBus.getDefault().post(new LoadMoreEvent());
                    }
                });
            }
        }

        if(nodes.size() <=0 ){
            showEmptyView(view);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nodesAdapter = new NodesAdapter(getActivity(), nodes, pickedFiles);
        if (viewType.equals(Constants.THUMBNAILS_VIEW)) {
            nodesAdapter.setThumbnail(true);
        }

        recyclerView.setAdapter(nodesAdapter);
        nodesAdapter.setNodeClickListener(new NodesAdapter.OnNodeClickListener() {
            @Override
            public void onNodeClick(Node node) {
                // If node is dir then open it
                if (node.isDir) {
                    openDir(node);
                } else {
                    // Proceed single file
                    if(node instanceof GoogleDriveNode){
                        GoogleDriveNode gNode = (GoogleDriveNode)node;
                        if(gNode.driveType.equals(Constants.TYPE_DRIVE)){
                            if(Filepicker.getDriveAbout() != null) {
                                if (Filepicker.getDriveAbout().getExportFormats().containsKey(gNode.mimeType)) {
                                    List<String> formats = Filepicker.getDriveAbout().getExportFormats().get(gNode.mimeType);
                                    if (formats.size() == 1) {
                                        gNode.exportFormat = formats.get(0);
                                        selectFileNode(node);
                                    } else {
                                        selectFormat(gNode, formats);
                                    }
                                } else {
                                    if(gNode.mimeType.contains("google-apps")){
                                        Toast.makeText(getActivity(),"This File cannot be exported",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    gNode.exportFormat = gNode.mimeType;
                                    selectFileNode(node);
                                }
                            }else{
                                Toast.makeText(getActivity(),"Export formats not Available",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            selectFileNode(gNode);
                        }
                    }else{
                        selectFileNode(node);
                    }

                }

            }
        });

        mUploadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pickedFiles.isEmpty()) {
                    showProgress();
                    getContract().pickFiles(PickedFile.getNodes(pickedFiles));
                }
            }
        });
    }

    private void selectFormat(final GoogleDriveNode node, List<String> formatList){

        ArrayAdapter<Constants.ExportObject> adapter  =
                new ArrayAdapter<>(getActivity(),
                        android.R.layout.select_dialog_singlechoice);
        final ArrayList<String> formatFilterList = new ArrayList<>();

        for(String format : formatList){
            if(Constants.exportMap.get(format) != null) {
                formatFilterList.add(format);
                adapter.add(Constants.exportMap.get(format));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.message_dialog_select_format).setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                node.exportFormat = formatFilterList.get(which);
                selectFileNode(node);
            }
        });

        builder.show();

    }

    private void selectFileNode(Node node){
        PickedFile pickedFile = new PickedFile(node);
        if (canPickMultiple()) {
            updatePickedList(pickedFile);
            setUploadButton();
        } else {
            // updatePickedList(pickedFile);
            uploadSingleFile(pickedFile);
        }

        nodesAdapter.notifyDataSetChanged();

    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void setUploadButton() {
        if (!pickedFiles.isEmpty() && (maxFiles() < 0 || pickedFiles.size() <= maxFiles())) {
            mUploadFilesButton.setVisibility(View.VISIBLE);

            String btnText = (pickedFiles.size() == 1) ? "Upload 1 file" : "Upload " + pickedFiles.size() + " files";
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
        if (id == R.id.action_logout) {
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
        if (node.isGallery()) {
            getContract().openGallery(canPickMultiple());
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
        recyclerView.setEnabled(false);
    }

    private void updatePickedList(PickedFile pickedFile) {
        if (PickedFile.containsNode(pickedFiles, pickedFile.node)) {
            PickedFile.removeNode(pickedFiles, pickedFile.node);
        } else {
            pickedFiles.add(pickedFile);
        }
    }

    private boolean canPickMultiple() {
        return PreferencesUtils.newInstance(getActivity()).getMultiple();
    }

    private Integer maxFiles() {
        return PreferencesUtils.newInstance(getActivity()).getMaxFiles();
    }

    Contract getContract() {
        return (Contract) getActivity();
    }

    // Activity needs to implement these methods
    public interface Contract {

        void openCamera();

        void openGallery(boolean allowMultiple);

        void pickFiles(ArrayList<Node> node);

        void showNextNode(Node node);

        void logoutUser();
    }

}