package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.TraceFile;

public class SessionDownloadWidget extends Composite {

    private static SessionDownloadWidgetUiBinder uiBinder = GWT
            .create(SessionDownloadWidgetUiBinder.class);

    interface SessionDownloadWidgetUiBinder extends UiBinder<Widget, SessionDownloadWidget> {
    }
    
    @UiField CellTable<TraceFile> traceTable;
    @UiField NavLink linkStatsDownload;
    
    private Session mSession = null;
    
    private ListDataProvider<TraceFile> mDataProvider = new ListDataProvider<TraceFile>();

    public SessionDownloadWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create the cell-table
        createTraceTable();
    }

    public void initialize(final Session session) {
        // store session globally
        mSession = session;
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
        
        // refresh table data
        refreshTraceTable(mSession);
    }
    
    @UiHandler("linkStatsDownload")
    public void onStatsDownloadClick(ClickEvent evt) {
        // TODO: download stats data dump
    }
    
    private void createTraceTable() {
        mDataProvider = new ListDataProvider<TraceFile>();
        mDataProvider.addDataDisplay(traceTable);
        
        // set table name
        traceTable.setTitle("Trace files");
        
        /**
         * name column
         */
        TextColumn<TraceFile> nameColumn = new TextColumn<TraceFile>() {
            @Override
            public String getValue(TraceFile f) {
                return f.filename;
            }
        };

        traceTable.addColumn(nameColumn, "Name");
        
        /**
         * modified column
         */
        TextColumn<TraceFile> modifiedColumn = new TextColumn<TraceFile>() {
            @Override
            public String getValue(TraceFile f) {
                return formatDatetime(f.modified);
            }
        };

        traceTable.addColumn(modifiedColumn, "Modified");
        traceTable.setColumnWidth(modifiedColumn, 12, Unit.EM);
        
        /**
         * size column
         */
        TextColumn<TraceFile> sizeColumn = new TextColumn<TraceFile>() {
            @Override
            public String getValue(TraceFile f) {
                return humanReadableByteCount(f.size, true);
            }
        };

        traceTable.addColumn(sizeColumn, "Size");
        traceTable.setColumnWidth(sizeColumn, 6, Unit.EM);
    }
    
    private void refreshTraceTable(Session s) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getTraceFiles(s, new AsyncCallback<ArrayList<TraceFile>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<TraceFile> result) {
                List<TraceFile> list = mDataProvider.getList();
                list.clear();
                for (TraceFile tf : result) {
                    // update table data
                    list.add(tf);
                }
            }
        });
    }
    
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        Double volume = bytes / Math.pow(unit, exp);
        return NumberFormat.getFormat("0.00").format(volume) + " " + pre;
    }
    
    public static String formatDatetime(long timestamp) {
        return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT).format(new Date(timestamp));
    }
}
