package io.filepicker;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
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

import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.Utils;
import io.filepicker.utils.ViewUtils;

/**
 * Created by maciejwitowski on 11/7/14.
 */
public class ExportFragment extends Fragment {

    // Activity needs to implement these methods
    public interface Contract {
        public void showContent(Node node);
        public void exportFile(String fileName);
        public Uri getFileToExport();
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
    NodesAdapter<Node> nodesAdapter;
    TextView mFileType;
    EditText etFilename;
    Button mBtnSave;


    // Layout with edit text for filename and save button
    LinearLayout mExportForm;

    public static ExportFragment newInstance(Node parentNode, Node[] nodes, String viewType) {
        ExportFragment frag = new ExportFragment();
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

        // If we're in provider root folder (first folder in given provider)
        if(parentNode != null && Utils.isProvider(parentNode)){
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_export, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBarNode);
        mExportForm = (LinearLayout) view.findViewById(R.id.exportForm);
        mFileType = (TextView) view.findViewById(R.id.fileType);
        etFilename = (EditText) view.findViewById(R.id.etFilename);
        mBtnSave = (Button) view.findViewById(R.id.btnSave);

        if(viewType.equals(LIST_VIEW)) {
            currentView = (ListView) view.findViewById(R.id.listView);
        } else if(viewType.equals(THUMBNAILS_VIEW)) {
            currentView = (GridView) view.findViewById(R.id.gridView);
        } else {
            showEmptyView(view);
        }

        // Show edit text for filename and save button if we're in content (not in providers list)
        if(parentNode != null) {
            ViewUtils.show(mExportForm);
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
                    getContract().showContent(node);
                }
            }
        });

        currentView.setVisibility(View.VISIBLE);

        // Form consists of input for name, text with extension and save button
        initForm();
    }

    private void initForm() {
        if(parentNode != null) {
            mFileType.setText("." + typeOfExportFile());
        }

        etFilename.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable text) {
                String value = text.toString() + "." + typeOfExportFile();

                if(Node.nameExists(nodes, value)) {
                    showOverrideBtn();
                } else {
                    showSaveBtn();
                }
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
                if(etFilename.getText().length() > 0) {
                    String filename = etFilename.getText().toString();
                    getContract().exportFile(filename);
                }
            }
        });
    }

    private void showSaveBtn() {
        mBtnSave.setText(getResources().getText(R.string.save));
        mBtnSave.setBackgroundColor(getResources().getColor(R.color.blue));
    }

    private void showOverrideBtn() {
        mBtnSave.setText(getResources().getText(R.string.override));
        mBtnSave.setBackgroundColor(getResources().getColor(R.color.yellow));
    }

    private void showEmptyView(View view) {
        ViewUtils.show(view.findViewById(R.id.emptylistView));
    }

    public Contract getContract() {
        return (Contract) getActivity();
    }

    private String typeOfExportFile() {
        return FilesUtils.getFileExtension(getActivity(), getContract().getFileToExport());
    }

}
