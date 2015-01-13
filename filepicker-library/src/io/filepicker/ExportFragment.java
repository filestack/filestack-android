package io.filepicker;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import io.filepicker.adapters.NodesAdapter;
import io.filepicker.models.FPFile;
import io.filepicker.models.Node;
import io.filepicker.models.PickedFile;
import io.filepicker.utils.Constants;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.Utils;
import io.filepicker.utils.ViewUtils;

/**
 * Created by maciejwitowski on 11/7/14.
 */
public class ExportFragment extends Fragment {

    // Activity needs to implement these methods
    public interface Contract {
        public void showNextNode(Node node);
        public void exportFile(String fileName);
        public Uri getFileToExport();
    }

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

    private AbsListView currentView;
    private ProgressBar mProgressBar;
    private NodesAdapter<Node> nodesAdapter;
    private TextView mFileType;
    private EditText etFilename;
    private Button mBtnSave;


    // Layout with edit text for filename and save button
    private LinearLayout mExportForm;

    public static ExportFragment newInstance(Node parentNode, ArrayList<Node> nodes, String viewType) {
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

        if (bundle == null) getActivity().finish();

        viewType = bundle.getString(KEY_VIEW_TYPE);

        nodes = bundle.getParcelableArrayList(KEY_NODES);
        if(nodes == null) {
            nodes = new ArrayList<>();
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

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mExportForm = (LinearLayout) view.findViewById(R.id.exportForm);
        mFileType = (TextView) view.findViewById(R.id.fileType);
        etFilename = (EditText) view.findViewById(R.id.etFilename);
        mBtnSave = (Button) view.findViewById(R.id.btnSave);

        switch (viewType) {
            case LIST_VIEW:
                currentView = (ListView) view.findViewById(R.id.listView);
                break;
            case THUMBNAILS_VIEW:
                currentView = (GridView) view.findViewById(R.id.gridView);
                break;
            default:
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
                if (node.isDir) {
                    getContract().showNextNode(node);
                }
            }
        });

        currentView.setVisibility(View.VISIBLE);

        // Form consists of input for name, text with extension and save button
        initForm();
    }

    private void initForm() {
        if(parentNode != null) {
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
                if(etFilename.getText().length() > 0) {
                    currentView.setEnabled(false);
                    currentView.setAlpha(Constants.ALPHA_FADED);
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

        if(Node.nameExists(nodes, value)) {
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
}
