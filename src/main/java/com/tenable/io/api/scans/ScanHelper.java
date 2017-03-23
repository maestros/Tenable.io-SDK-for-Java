package com.tenable.io.api.scans;


import com.tenable.io.api.TenableIoClient;
import com.tenable.io.api.editors.EditorApi;
import com.tenable.io.api.editors.models.Template;
import com.tenable.io.api.editors.models.TemplateType;
import com.tenable.io.api.folders.FolderRef;
import com.tenable.io.api.scans.interfaces.RunnableScan;
import com.tenable.io.core.exceptions.TenableIoException;
import com.tenable.io.core.exceptions.TenableIoErrorCode;
import com.tenable.io.api.scans.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Copyright (c) 2017 Tenable Network Security, Inc.
 */
public class ScanHelper {
    private static Logger logger = LoggerFactory.getLogger( ScanHelper.class );

    /**
     * The string literal indicating a scan export request is ready for download.
     */
    public final String STATUS_EXPORT_READY = "ready";

    /**
     * The list of Statuses indicating a scan is stopped.
     */
    public final List<ScanStatus> STATUSES_STOPPED = Arrays.asList( new ScanStatus[]{
            ScanStatus.STOPPED, ScanStatus.ABORTED, ScanStatus.COMPLETED, ScanStatus.CANCELED, ScanStatus.EMPTY,
            ScanStatus.IMPORTED
    } );

    private TenableIoClient client;

    private int sleepInterval = 5000;


    /**
     * Instantiates a new Scan helper.
     *
     * @param client the client
     */
    public ScanHelper( TenableIoClient client ) {
        this.client = client;
    }


    /**
     * Create a new scan
     *
     * @param scanName The name of the Scan to be created.
     * @param targets  A list of scan targets separated by commas.
     * @param template The name or title of the template, or an instance of Template.
     * @return ScanRef referenced by id if exists.
     * @throws TenableIoException the tenable IO exception
     */
    public RunnableScan createScan( String scanName, String targets, String template ) throws TenableIoException {
        Template tmplt = getTemplateByName( template );
        if( tmplt == null ) {
            tmplt = getTemplateByTitle( template );
        }

        if( tmplt == null ) {
            throw new TenableIoException( TenableIoErrorCode.Generic, "scan template name not found" );
        }

        ScansApi scanApi = client.getScansApi();
        Settings settings = new Settings();
        settings.setTextTargets( targets );
        settings.setName( scanName );
        ScanResult result = scanApi.create( tmplt.getUuid(), settings );
        return new ScanRef( this.client, result.getId() );
    }


    /**
     * Get template by name
     *
     * @param name The name of the template.
     * @return An instance of Template if exists, otherwise Null.
     * @throws TenableIoException the Tenable IO exception
     */
    public Template getTemplateByName( String name ) throws TenableIoException {
        return template( name, null );
    }


    /**
     * Get template by title
     *
     * @param title The title of the template.
     * @return An instance of Template if exists, otherwise Null.
     * @throws TenableIoException the Tenable IO exception
     */
    public Template getTemplateByTitle( String title ) throws TenableIoException {
        return template( null, title );
    }


    /**
     * Get a scan by id.
     *
     * @param id the id of the scan
     * @return the scan ref object
     * @throws TenableIoException the tenable IO exception
     */
    public ScanRef getScan( int id ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanDetails details = scanApi.details( id );
        return new ScanRef( this.client, details.getInfo().getObjectId() );
    }


    /**
     * Return list of scans
     *
     * @return the list of scans
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScans() throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list();
        return getScans( allScans, null, null );
    }


    /**
     * Return list of scans inside a folder
     *
     * @param folderId the folder id to search scans under
     * @return the list of scans
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScans( int folderId ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list( folderId );
        return getScans( allScans, null, null );
    }


    /**
     * Return list of scans by name.
     *
     * @param name the name to search for
     * @return the list of scans
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScansByName( String name ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list();
        return getScans( allScans, name, null );
    }


    /**
     * Return list of scans by name.
     *
     * @param name     the name to search for
     * @param folderId the folder id to search scans under
     * @return the list of scans
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScansByName( String name, int folderId ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list( folderId );
        return getScans( allScans, name, null );
    }


    /**
     * Return list of scans with names matching a regular expression
     *
     * @param nameRegex the name regular expression pattern
     * @return the list of scans matching the regular expression
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScansByRegex( String nameRegex ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list();
        return getScans( allScans, null, nameRegex );
    }


    /**
     * Return list of scans with names matching a regular expression
     *
     * @param nameRegex the name regular expression pattern
     * @param folderId  the folder id to search scans under
     * @return the list of scans matching the regular expression
     * @throws TenableIoException the tenable IO exception
     */
    public List<ScanRef> getScansByRegex( String nameRegex, int folderId ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();
        ScanListResult allScans = scanApi.list( folderId );
        return getScans( allScans, null, nameRegex );
    }


    /**
     * Stop existing scans and wait until all are stopped.
     *
     * @throws TenableIoException the tenable IO exception
     */
    public void stopAll() throws TenableIoException {
        List<ScanRef> scans = getScans();
        stopAll( scans );
    }


    /**
     * Stop existing scans and wait until all are stopped.
     *
     * @param folderId stop scans under this folder only
     * @throws TenableIoException the tenable IO exception
     */
    public void stopAll( int folderId ) throws TenableIoException {
        List<ScanRef> scans = getScans( folderId );
        stopAll( scans );
    }


    /**
     * Stop existing scans and wait until all are stopped.
     *
     * @param folder Instance of FolderRef. Stop all scan in the folder only.
     * @throws TenableIoException the tenable IO exception
     */
    public void stopAll( FolderRef folder ) throws TenableIoException {
        stopAll( folder.getId() );
    }


    /**
     * Stop scans and wait until all are stopped.
     *
     * @param scans List of ScanRef. Stop only this list of scans
     * @throws TenableIoException the tenable IO exception
     */
    public void stopAll( List<ScanRef> scans ) throws TenableIoException {
        for( ScanRef item : scans ) {
            try {
                item.stop( false );
            } catch( TenableIoException e ) {
            }
        }
        for( ScanRef item : scans ) {
            item.waitUntilStopped();
        }
    }


    /**
     * Iterates through all the scans and trims the scan history. Leaves the most recent numMostRecentToKeep history.
     * Setting numMostRecentToKeep to 0 will delete all scan history.
     *
     * @param numMostRecentToKeep the number of most recent history to keep per scan
     * @throws TenableIoException the tenable io exception
     */
    public void trimScanHistory( int numMostRecentToKeep ) throws TenableIoException {
        ScansApi scanApi = client.getScansApi();

        ScanListResult result = scanApi.list();
        logger.debug( String.format( "Found %d scans.", result.getScans().size() ) );

        Comparator<History> comparator = new ScanHistoryLastModifiedDateDescending();

        for( Scan scan : result.getScans() ) {
            ScanDetails details = scanApi.details( scan.getId() );

            // more than 100 histories?
            if( details.getHistories() != null && details.getHistories().size() > numMostRecentToKeep ) {
                // first order them by timestamp
                List<History> histories = new ArrayList<>( details.getHistories() );
                Collections.sort( histories, comparator );

                logger.info( String.format( "Scan ID# %d has %d histories. Trimming oldest %d histories.", scan.getId(), details.getHistories().size(), details.getHistories().size() - numMostRecentToKeep ) );

                // trim histories
                for( int i = numMostRecentToKeep; i < histories.size(); i++ ) {
                    try {
                        scanApi.deleteHistory( scan.getId(), histories.get( i ).getHistoryId() );
                    } catch( TenableIoException te ) {
                        // log and continue
                        logger.error( String.format( "Error while trying to delete scan history. Scan ID# %d, history ID: %d.", scan.getId(), histories.get( i ).getHistoryId() ) );
                    }
                }
            }
        }
    }


    /**
     * Gets sleep interval.
     *
     * @return the sleep interval
     */
    public int getSleepInterval() {
        return sleepInterval;
    }


    /**
     * Sets sleep interval.
     *
     * @param sleepInterval the sleep interval
     */
    public void setSleepInterval( int sleepInterval ) {
        this.sleepInterval = sleepInterval;
    }


    private List<ScanRef> getScans( ScanListResult scans, String name, String regex ) throws TenableIoException {
        List<ScanRef> result = new ArrayList<>();
        if( scans != null && scans.getScans() != null ) {
            for( Scan scan : scans.getScans() ) {
                if( name != null ) {
                    if( scan.getName().equals( name ) ) {
                        result.add( new ScanRef( this.client, scan.getId() ) );
                    }
                } else if( regex != null ) {
                    if( scan.getName().matches( regex ) ) {
                        result.add( new ScanRef( this.client, scan.getId() ) );
                    }
                } else {
                    result.add( new ScanRef( this.client, scan.getId() ) );
                }
            }
        }
        return result;
    }


    private Template template( String name, String title ) throws TenableIoException {
        Template result = null;
        EditorApi editor = client.getEditorApi();
        List<Template> editors = editor.list( TemplateType.SCAN );
        for( Template item : editors ) {
            if( name != null ) {
                if( item.getName().equals( name ) ) {
                    result = item;
                    break;
                }
            } else if( title != null ) {
                if( item.getTitle().equals( title ) ) {
                    result = item;
                    break;
                }
            }
        }
        return result;
    }


    private class ScanHistoryLastModifiedDateDescending implements Comparator<History> {
        public int compare( History h1, History h2 ) {
            return Integer.compare( h2.getLastModificationDate(), h1.getLastModificationDate() );
        }
    }
}
