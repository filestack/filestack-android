package io.filepicker;

import android.app.Fragment;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.FPFile;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.Constants;
import io.filepicker.utils.EndlessRecyclerViewScrollListener;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.LoadMoreEvent;
import io.filepicker.utils.Utils;
import io.filepicker.utils.ViewUtils;

/**
 * Created by maciejwitowski on 11/7/14.
 */
public class ExportFragment extends Fragment {

    private static final String KEY_NODES = "nodes";
    private static final String KEY_PARENT_NODE = "parent_node";
    private static final String KEY_VIEW_TYPE = "viewType";

    private static final String LIST_VIEW   = "list";
    private static final String THUMBNAILS_VIEW  = "thumbnails";

    private String viewType;
    private ArrayList<Node> nodes;
    private Node parentNode;

    // Used when user can pick many files at once
    private final ArrayList<PickedFile> pickedFiles = new ArrayList<>();

    private RecyclerView recyclerView;
    private ProgressBar mProgressBar;
    private TextView mFileType;
    private EditText etFilename;
    private Button mBtnSave;

    public static ExportFragment newInstance(Node parentNode, ArrayList<? extends Node> nodes, String viewType) {
        ExportFragment frag = new ExportFragment();
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

        Bundle bundle = getArguments();
        if (bundle == null) {
            getActivity().finish();
            return;
        }

        viewType = bundle.getString(KEY_VIEW_TYPE);

        nodes = bundle.getParcelableArrayList(KEY_NODES);
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        parentNode = bundle.getParcelable(KEY_PARENT_NODE);

        // If we're in provider root folder (first folder in given provider)
        if (parentNode != null && Utils.isProvider(parentNode)) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export, container, false);
        RecyclerView.LayoutManager layoutManager = null;

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mFileType = (TextView) view.findViewById(R.id.fileType);
        etFilename = (EditText) view.findViewById(R.id.etFilename);
        mBtnSave = (Button) view.findViewById(R.id.btnSave);

        recyclerView = (RecyclerView)view.findViewById(R.id.recycler_list);
        recyclerView.setHasFixedSize(true);

        switch (viewType) {
            case LIST_VIEW:
                recyclerView .setLayoutManager(layoutManager = new LinearLayoutManager(getActivity()));
                break;
            case THUMBNAILS_VIEW:
                recyclerView.setLayoutManager(layoutManager = new GridLayoutManager(getActivity(),3,GridLayoutManager.VERTICAL,false));
                break;
            default:
                showEmptyView(view);
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

        // Show edit text for filename and save button if we're in content (not in providers list)
        if (parentNode != null) {
            LinearLayout exportForm = (LinearLayout) view.findViewById(R.id.exportForm);
            ViewUtils.show(exportForm);
        }

        if(nodes.size() <=0 ){
            showEmptyView(view);
        }


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NodesAdapter nodesAdapter = new NodesAdapter(getActivity(), nodes, pickedFiles);

        if (viewType.equals(THUMBNAILS_VIEW)) {
            nodesAdapter.setThumbnail(true);
        }

        recyclerView.setAdapter(nodesAdapter);
        nodesAdapter.setNodeClickListener(new NodesAdapter.OnNodeClickListener(){

            @Override
            public void onNodeClick(Node node) {
                // If node is dir then open it
                if (node.isDir) {
                    getContract().showNextNode(node);
                }

            }
        });

        // Form consists of input for name, text with extension and save button
        initForm();
    }

    private void initForm() {
        if (parentNode != null) {
            Uri fileUri = getContract().getFileToExport();

            mFileType.setText("." + FilesUtils.getFileExtension(getActivity(),fileUri));
            String filename = FPFile.contentUriToFilename(fileUri);

            etFilename.setText(Utils.filenameWithoutExtension(filename));
            showSaveButton(filename);
        }

        etFilename.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable text) {
                showSaveButton(text.toString() + "." + typeOfExportFile());
            }

            // Not used
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            // Not used
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etFilename.getText().length() > 0) {
                    recyclerView.setEnabled(false);
                    recyclerView.setAlpha(Constants.ALPHA_FADED);
                    etFilename.setEnabled(false);
                    mBtnSave.setEnabled(false);
                    mBtnSave.setText("Saving file...");
                    mProgressBar.setVisibility(View.VISIBLE);

                    String filename = etFilename.getText().toString();
                    getContract().exportFile(filename);
                }
            }
        });
    }

    private void showSaveButton(String value) {
        int textRes;
        int backgroundColor;

        if (Node.nameExists(nodes, value)) {
            textRes = R.string.override;
            backgroundColor = R.color.yellow;
        } else {
            textRes = R.string.save;
            backgroundColor = R.color.blue;
        }

        Resources res = getResources();
        mBtnSave.setText(res.getText(textRes));
        mBtnSave.setBackgroundColor(res.getColor(backgroundColor));
    }

    private void showEmptyView(View view) {
        ViewUtils.show(view.findViewById(R.id.emptylistView));
    }

    Contract getContract() {
        return (Contract) getActivity();
    }

    private String typeOfExportFile() {
        return FilesUtils.getFileExtension(getActivity(), getContract().getFileToExport());
    }

    // Activity needs to implement these methods
    public interface Contract {

        void showNextNode(Node node);

        void exportFile(String fileName);

        Uri getFileToExport();

    }
}
