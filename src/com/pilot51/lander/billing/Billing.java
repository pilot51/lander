package com.pilot51.lander.billing;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.pilot51.lander.Main;
import com.pilot51.lander.Options;
import com.pilot51.lander.R;
import com.pilot51.lander.billing.BillingService.RequestPurchase;
import com.pilot51.lander.billing.BillingService.RestoreTransactions;
import com.pilot51.lander.billing.Consts.PurchaseState;
import com.pilot51.lander.billing.Consts.ResponseCode;

public class Billing {
	private static final String TAG = Main.TAG + "-Billing";
	
	/**
	 * The SharedPreferences key for recording whether we initialized the database. If false, then
	 * we perform a RestoreTransactions request to get all the purchases for this user.
	 */
	private static final String DB_INITIALIZED = "db_initialized";

	private Activity activity;
	private LanderPurchaseObserver mLanderPurchaseObserver;
	private Handler mHandler;
	private BillingService mBillingService;
	private PurchaseDatabase mPurchaseDatabase;
	private Set<String> mOwnedItems = new HashSet<String>();
	
	public static boolean bReady;

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;

	/**
	 * Each product in the catalog is either MANAGED or UNMANAGED. MANAGED means that the product
	 * can be purchased only once per user (such as a new level in a game). The purchase is
	 * remembered by Android Market and can be restored if this application is uninstalled and then
	 * re-installed. UNMANAGED is used for products that can be used up and purchased multiple times
	 * (such as poker chips). It is up to the application to keep track of UNMANAGED products for
	 * the user.
	 */
	private enum Managed {
		MANAGED, UNMANAGED
	}

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market sends messages to
	 * this application so that we can update the UI.
	 */
	private class LanderPurchaseObserver extends PurchaseObserver {
		public LanderPurchaseObserver(Handler handler) {
			super(activity, handler);
		}

		@Override
		public void onBillingSupported(boolean supported) {
			if (Consts.DEBUG) Log.i(TAG, "supported: " + supported);
			if (supported) {
				restoreDatabase();
				bReady = !isOwned("unlock");
			} else createDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity,
			long purchaseTime, String developerPayload) {
			if (Consts.DEBUG)
				Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
			if (purchaseState == PurchaseState.PURCHASED) mOwnedItems.add(itemId);
			mCatalogAdapter.setOwnedItems(mOwnedItems);
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
			if (Consts.DEBUG) Log.d(TAG, request.mProductId + ": " + responseCode);
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) Log.i(TAG, "purchase was successfully sent to server");
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				if (Consts.DEBUG) Log.i(TAG, "user canceled purchase");
			} else {
				if (Consts.DEBUG) Log.i(TAG, "purchase failed");
			}
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
			ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) Log.d(TAG, "completed RestoreTransactions request");
				// Update the shared preferences so that we don't perform
				// a RestoreTransactions again.
				Main.prefs.edit().putBoolean(DB_INITIALIZED, true).commit();
			} else if (Consts.DEBUG) Log.d(TAG, "RestoreTransactions error: " + responseCode);
		}
	}

	private static class CatalogEntry {
		public String sku;
		public int nameId;
		public Managed managed;

		public CatalogEntry(String sku, int nameId, Managed managed) {
			this.sku = sku;
			this.nameId = nameId;
			this.managed = managed;
		}
	}

	/** An array of product list entries for the products that can be purchased. */
	private static final CatalogEntry[] CATALOG =
		new CatalogEntry[] {
			new CatalogEntry("unlock", R.string.unlock, Managed.MANAGED),
			new CatalogEntry("android.test.purchased", R.string.android_test_purchased,
				Managed.MANAGED),
			new CatalogEntry("android.test.canceled", R.string.android_test_canceled,
				Managed.MANAGED),
			new CatalogEntry("android.test.refunded", R.string.android_test_refunded,
				Managed.MANAGED),
			new CatalogEntry("android.test.item_unavailable",
				R.string.android_test_item_unavailable, Managed.MANAGED),};

	private CatalogAdapter mCatalogAdapter;

	/**
	 * @param a
	 *            Activity for dialogs and such.
	 * @param pref
	 *            Preference to be modified according to purchase state.
	 */
	public Billing(Activity a) {
		activity = a;
		mHandler = new Handler();
		mLanderPurchaseObserver = new LanderPurchaseObserver(mHandler);
		mBillingService = new BillingService();
		mBillingService.setContext(activity);
		mPurchaseDatabase = new PurchaseDatabase(activity);
		bReady = false;
		mCatalogAdapter = new CatalogAdapter(activity, CATALOG);

		// Check if billing is supported.
		ResponseHandler.register(mLanderPurchaseObserver);
		if (!mBillingService.checkBillingSupported()) createDialog(DIALOG_CANNOT_CONNECT_ID);

		initializeOwnedItems();
	}

	public void onStart() {
		ResponseHandler.register(mLanderPurchaseObserver);
		initializeOwnedItems();
	}

	public void onStop() {
		ResponseHandler.unregister(mLanderPurchaseObserver);
	}

	public void onDestroy() {
		mPurchaseDatabase.close();
		mBillingService.unbind();
	}

	private void createDialog(int id) {
		switch (id) {
			case DIALOG_CANNOT_CONNECT_ID:
				createDialog(R.string.cannot_connect_title, R.string.cannot_connect_message).show();
			case DIALOG_BILLING_NOT_SUPPORTED_ID:
				createDialog(R.string.billing_not_supported_title,
					R.string.billing_not_supported_message).show();
		}
	}

	private Dialog createDialog(int titleId, int messageId) {
		String helpUrl = replaceLanguageAndRegion(activity.getString(R.string.help_url));
		if (Consts.DEBUG) Log.i(TAG, helpUrl);
		final Uri helpUri = Uri.parse(helpUrl);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(titleId).setIcon(android.R.drawable.stat_sys_warning)
			.setMessage(messageId).setCancelable(false)
			.setPositiveButton(android.R.string.ok, null)
			.setNegativeButton(R.string.learn_more, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
					activity.startActivity(intent);
				}
			});
		return builder.create();
	}

	/**
	 * Replaces the language and/or country of the device into the given string. The pattern
	 * "%lang%" will be replaced by the device's language code and the pattern "%region%" will be
	 * replaced with the device's country code.
	 * 
	 * @param str
	 *            the string to replace the language/country within
	 * @return a string containing the local language and region codes
	 */
	private String replaceLanguageAndRegion(String str) {
		// Substitute language and or region if present in string
		if (str.contains("%lang%") || str.contains("%region%")) {
			Locale locale = Locale.getDefault();
			str = str.replace("%lang%", locale.getLanguage().toLowerCase());
			str = str.replace("%region%", locale.getCountry().toLowerCase());
		}
		return str;
	}

	/**
	 * If the database has not been initialized, we send a RESTORE_TRANSACTIONS request to Android
	 * Market to get the list of purchased items for this user. This happens if the application has
	 * just been installed or the user wiped data. We do not want to do this on every startup,
	 * rather, we want to do only when the database needs to be initialized.
	 */
	private void restoreDatabase() {
		if (!Main.prefs.getBoolean(DB_INITIALIZED, false)) {
			mBillingService.restoreTransactions();
			//Toast.makeText(activity, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Creates a background thread that reads the database and initializes the set of owned items.
	 */
	private void initializeOwnedItems() {
		new Thread(new Runnable() {
			public void run() {
				doInitializeOwnedItems();
			}
		}).start();
	}

	/**
	 * Reads the set of purchased items from the database in a background thread and then adds those
	 * items to the set of owned items in the main UI thread.
	 */
	private void doInitializeOwnedItems() {
		Cursor cursor = mPurchaseDatabase.queryAllPurchasedItems();
		if (cursor == null) return;
		final Set<String> ownedItems = new HashSet<String>();
		try {
			int productIdCol =
				cursor.getColumnIndexOrThrow(PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
			while (cursor.moveToNext()) {
				String productId = cursor.getString(productIdCol);
				ownedItems.add(productId);
			}
		} finally {
			cursor.close();
		}

		// We will add the set of owned items in a new Runnable that runs on
		// the UI thread so that we don't need to synchronize access to
		// mOwnedItems.
		mHandler.post(new Runnable() {
			public void run() {
				mOwnedItems.addAll(ownedItems);
				mCatalogAdapter.setOwnedItems(mOwnedItems);
			}
		});
	}

	public void purchase(String itemSku) {
		String mItemName = null, mSku = null;
		for (int i = 0; i < CATALOG.length; i++) {
			if (CATALOG[i].sku.equals(itemSku)) {
				mItemName = activity.getString(CATALOG[i].nameId);
				mSku = CATALOG[i].sku;
				break;
			}
		}
		if (Consts.DEBUG) Log.d(TAG, "buying: " + mItemName + " sku: " + mSku);
		if (!mBillingService.requestPurchase(mSku, null))
			createDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
	}

	public boolean isOwned(String itemSku) {
		boolean owned = mOwnedItems.contains(itemSku);
		if (Consts.DEBUG) Log.d(TAG, "own sku: " + itemSku + " - " + owned);
		return owned;
	}

	/**
	 * An adapter used for displaying a catalog of products. If a product is managed by Android
	 * Market and already purchased, then it will be "grayed-out" in the list and not selectable.
	 */
	private static class CatalogAdapter extends ArrayAdapter<String> {
		private CatalogEntry[] mCatalog;
		private Set<String> mOwnedItems = new HashSet<String>();

		public CatalogAdapter(Context context, CatalogEntry[] catalog) {
			super(context, android.R.layout.simple_spinner_item);
			mCatalog = catalog;
			for (CatalogEntry element : catalog) {
				add(context.getString(element.nameId));
			}
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		public void setOwnedItems(Set<String> ownedItems) {
			mOwnedItems = ownedItems;
			notifyDataSetChanged();
			if (mOwnedItems.contains("unlock")) {
				if (Main.prefs.getInt("unlock", Options.UNLOCK_OFF) == Options.UNLOCK_OFF)
					Main.prefs.edit().putInt("unlock", Options.UNLOCK_PURCHASE).commit();
				bReady = false;
			} else if (Main.prefs.getInt("unlock", Options.UNLOCK_OFF) == Options.UNLOCK_PURCHASE)
				Main.prefs.edit().putInt("unlock", Options.UNLOCK_OFF).commit();
		}

		@Override
		public boolean areAllItemsEnabled() {
			// Return false to have the adapter call isEnabled()
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			// If the item at the given list position is not purchasable,
			// then prevent the list item from being selected.
			CatalogEntry entry = mCatalog[position];
			if (entry.managed == Managed.MANAGED && mOwnedItems.contains(entry.sku)) return false;
			return true;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			// If the item at the given list position is not purchasable, then
			// "gray out" the list item.
			View view = super.getDropDownView(position, convertView, parent);
			view.setEnabled(isEnabled(position));
			return view;
		}
	}
}
