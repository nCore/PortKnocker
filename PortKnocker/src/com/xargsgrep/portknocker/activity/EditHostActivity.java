package com.xargsgrep.portknocker.activity;

import java.util.regex.Pattern;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.xargsgrep.portknocker.R;
import com.xargsgrep.portknocker.fragment.HostFragment;
import com.xargsgrep.portknocker.fragment.MiscFragment;
import com.xargsgrep.portknocker.fragment.PortsFragment;
import com.xargsgrep.portknocker.manager.HostDataManager;
import com.xargsgrep.portknocker.model.Application;
import com.xargsgrep.portknocker.model.Host;
import com.xargsgrep.portknocker.model.Port;

public class EditHostActivity extends SherlockFragmentActivity implements ActionBar.TabListener {
	
	HostDataManager hostDataManager;
    
    // null when creating a new host
    private Long hostId;
    
	//public static final int MENU_ITEM_CANCEL = 1;
	public static final int MENU_ITEM_SAVE = 2;
	public static final int MENU_ITEM_ADD_PORT = 3;
	
	public static final int TAB_INDEX_HOST = 0;
	public static final int TAB_INDEX_PORTS = 1;
	public static final int TAB_INDEX_MISC = 2;
	
	public static final String HOST_ID_BUNDLE_KEY = "hostId";
	public static final String SELECTED_TAB_INDEX_BUNDLE_KEY = "selectedTabIndex";
	public static final String SAVE_HOST_RESULT_BUNDLE_KEY = "saveHostResult";
	
	private static final String HOST_TAB_FRAGMENT_TAG = "host_tab";
	private static final String PORTS_TAB_FRAGMENT_TAG = "ports_tab";
	private static final String MISC_TAB_FRAGMENT_TAG = "misc_tab";
	
    private static final int MAX_PORT_VALUE = 65535;

	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-z0-9]+([-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}$", Pattern.CASE_INSENSITIVE);
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_host);
        
        hostDataManager = new HostDataManager(this);
        
		Bundle extras = getIntent().getExtras();
		hostId = (extras != null && extras.containsKey(HOST_ID_BUNDLE_KEY)) ? extras.getLong(HOST_ID_BUNDLE_KEY) : null;
    	Host host = (hostId == null) ? null : hostDataManager.getHost(hostId);
		
    	if (host != null) getSupportActionBar().setSubtitle(host.getLabel());
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        addTab(getString(R.string.host_tab_name));
        addTab(getString(R.string.ports_tab_name));
        addTab(getString(R.string.misc_tab_name));
        
        if (savedInstanceState != null) {
        	getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(SELECTED_TAB_INDEX_BUNDLE_KEY));
        }
    }
    
    private void addTab(String text) {
        ActionBar.Tab tab = getSupportActionBar().newTab();
        tab.setText(text);
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);
    }
    
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Fragment hostFragment = getSupportFragmentManager().findFragmentByTag(HOST_TAB_FRAGMENT_TAG);
		Fragment portsFragment = getSupportFragmentManager().findFragmentByTag(PORTS_TAB_FRAGMENT_TAG);
		Fragment miscFragment = getSupportFragmentManager().findFragmentByTag(MISC_TAB_FRAGMENT_TAG);
		
		switch (tab.getPosition()) {
			case TAB_INDEX_HOST:
				if (hostFragment == null) {
					hostFragment = HostFragment.newInstance(hostId);
					ft.add(R.id.fragment_content, hostFragment, HOST_TAB_FRAGMENT_TAG);
				}
    			ft.show(hostFragment);
    			if (portsFragment != null) ft.hide(portsFragment);
    			if (miscFragment != null) ft.hide(miscFragment);
    			break;
			case TAB_INDEX_PORTS:
				if (portsFragment == null) {
					portsFragment = PortsFragment.newInstance(hostId);
					ft.add(R.id.fragment_content, portsFragment, PORTS_TAB_FRAGMENT_TAG);
				}
    			ft.show(portsFragment);
    			if (hostFragment != null) ft.hide(hostFragment);
    			if (miscFragment != null) ft.hide(miscFragment);
    			break;
			case TAB_INDEX_MISC:
				if (miscFragment == null) {
					miscFragment = MiscFragment.newInstance(hostId);
					ft.add(R.id.fragment_content, miscFragment, MISC_TAB_FRAGMENT_TAG);
				}
    			ft.show(miscFragment);
    			if (hostFragment != null) ft.hide(hostFragment);
    			if (portsFragment != null) ft.hide(portsFragment);
    			break;
		}
	}
	
	@Override
	public void onBackPressed() {
   		showCancelDialog();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//menu.add(Menu.NONE, MENU_ITEM_CANCEL, 1, "Cancel").setIcon(R.drawable.ic_action_cancel).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, MENU_ITEM_SAVE, 2, "Save").setIcon(R.drawable.ic_action_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	case android.R.id.home: 
	    		showCancelDialog();
		        return true;
	    	//case MENU_ITEM_CANCEL:
	    		//showCancelDialog();
	    		//return true;
	    	case MENU_ITEM_SAVE:
	    		saveHost();
	    		return true;
		    default:
		    	return false;
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt(SELECTED_TAB_INDEX_BUNDLE_KEY, getSupportActionBar().getSelectedNavigationIndex());
    }
    
    private void saveHost() {
		HostFragment hostFragment = (HostFragment) getSupportFragmentManager().findFragmentByTag(HOST_TAB_FRAGMENT_TAG);
		PortsFragment portsFragment = (PortsFragment) getSupportFragmentManager().findFragmentByTag(PORTS_TAB_FRAGMENT_TAG);
		MiscFragment miscFragment = (MiscFragment) getSupportFragmentManager().findFragmentByTag(MISC_TAB_FRAGMENT_TAG);
    	
    	Host host = (hostId == null) ? new Host() : hostDataManager.getHost(hostId);
    	
    	host.setLabel(hostFragment.getHostLabelEditText().getText().toString());
    	host.setHostname(hostFragment.getHostnameEditText().getText().toString());
    	
		if (portsFragment != null) { // could be null if user saves without going to ports tab
			host.setPorts(portsFragment.getPortsFromView());
		}
    	
		if (miscFragment != null) { // could be null if user saves without going to misc tab
			String delayStr = miscFragment.getDelayEditText().getText().toString();
			int delay = (delayStr != null && delayStr.length() > 0) ? Integer.parseInt(delayStr) : 0;
			host.setDelay(delay);
			
			Application application = (Application) miscFragment.getLaunchIntentSpinner().getSelectedItem();
			host.setLaunchIntentPackage(application.getIntent());
		}
		
		boolean validHostname = HOSTNAME_PATTERN.matcher(host.getHostname()).matches();
		boolean validIP = InetAddressUtils.isIPv4Address(host.getHostname());
		Toast toast = null;
		if (host.getLabel() == null || host.getLabel().length() == 0) {
			toast = Toast.makeText(this, "Please enter a label", Toast.LENGTH_SHORT);
		} else if (host.getHostname() == null || host.getHostname().length() == 0) {
			toast = Toast.makeText(this, "Please enter a hostname", Toast.LENGTH_SHORT);
		} else if (!validHostname && !validIP) {
			toast = Toast.makeText(this, "Invalid hostname/IP", Toast.LENGTH_SHORT);
		} else if (host.getPorts() == null || host.getPorts().size() == 0) {
			toast = Toast.makeText(this, "Please enter at least one port", Toast.LENGTH_SHORT);
		} else {
			for (Port port : host.getPorts()) {
				if (port.getPort() > MAX_PORT_VALUE) {
					toast = Toast.makeText(this, "Port is outside valid range", Toast.LENGTH_SHORT);
					break;
				}
			}
		}
		
		if (toast != null) {
			toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
			return;
		}
    	
    	boolean saveResult = (hostId == null) ? hostDataManager.saveHost(host) : hostDataManager.updateHost(host);
    	returnToHostListActivity(saveResult);
    }
    
    private void returnToHostListActivity(Boolean saveResult) {
		Intent hostListIntent = new Intent(this, HostListActivity.class);
		hostListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (saveResult != null) hostListIntent.putExtra(SAVE_HOST_RESULT_BUNDLE_KEY, saveResult);
        startActivity(hostListIntent);
    }
    
    private void showCancelDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.confirm_dialog_cancel_edit_title);
        dialogBuilder.setIcon(R.drawable.confirm_dialog_icon);
        
        dialogBuilder.setPositiveButton(R.string.confirm_dialog_confirm,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	returnToHostListActivity(null);
                }
            }
        );
        dialogBuilder.setNegativeButton(R.string.confirm_dialog_cancel,
    		new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) { }
    		}
        );
        
        dialogBuilder.create().show();
    }

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) { }

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) { }
    
}
