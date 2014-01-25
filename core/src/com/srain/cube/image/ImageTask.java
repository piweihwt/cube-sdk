package com.srain.cube.image;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.SparseArray;

import com.srain.cube.image.iface.ImageLoadHandler;
import com.srain.cube.util.Encrypt;

/**
 * A wrapper of the related information used in loading a bitmap
 * 
 * @author huqiu.lhq
 */
public class ImageTask {

	private static int sId = 0;;
	protected int mId;

	private static final String SIZE_SP = "_";

	protected String mUrl;
	protected HashMap<String, WeakReference<CubeImageView>> mRelatedImageViewList;

	protected Point mOriginSize = new Point();
	private Point mRequestSize = new Point();

	protected boolean mIsPreLoad = false;
	protected boolean mIsDoneOrAborted = false;
	protected boolean mIsLoading = false;

	protected SparseArray<String> mReuseCacheKeys = new SparseArray<String>();

	private String mIndentityKey;

	protected ImageReuseInfo mImageReuseInfo;

	public ImageTask(String url, int requestWidth, int requestHeight, ImageReuseInfo imageReuseInfo) {

		mId = ++sId;

		mUrl = url;
		mRelatedImageViewList = new HashMap<String, WeakReference<CubeImageView>>();
		if (imageReuseInfo != null) {
			mImageReuseInfo = imageReuseInfo;
		}
		mRequestSize = new Point(requestWidth, requestHeight);
		mIndentityKey = genSizeKey(mUrl, mRequestSize.x, mRequestSize.y);
	}

	public boolean isPreLoad() {
		return mIsPreLoad;
	}

	public void setPreLoad(boolean preload) {
		mIsPreLoad = preload;
	}

	public boolean isDoneOrAborted() {
		return mIsDoneOrAborted;
	}

	public void addRelatedImageView(CubeImageView imageView, ImageLoadHandler handler) {
		if (imageView == null) {
			return;
		}
		synchronized (mRelatedImageViewList) {
			final WeakReference<CubeImageView> imageWeakRef = new WeakReference<CubeImageView>(imageView);
			mRelatedImageViewList.put(imageView.toString(), imageWeakRef);
			if (mIsLoading && handler != null) {
				handler.onLoading(this, imageView);
			}
		}
	}

	public void removeRelatedImageView(CubeImageView imageView) {
		if (imageView == null) {
			return;
		}
		synchronized (mRelatedImageViewList) {
			mRelatedImageViewList.remove(imageView.toString());
		}
	}

	public boolean stillHasRelatedImageView() {
		synchronized (mRelatedImageViewList) {
			for (Iterator<Entry<String, WeakReference<CubeImageView>>> it = mRelatedImageViewList.entrySet().iterator(); it.hasNext();) {
				final CubeImageView imageView = it.next().getValue().get();
				if (imageView != null && equals(imageView.getHoldingImageTask())) {
					return true;
				}
			}
		}
		return false;
	}

	public void onLoading(ImageLoadHandler handler) {
		mIsLoading = true;
		if (null != handler) {
			synchronized (mRelatedImageViewList) {
				for (Iterator<Entry<String, WeakReference<CubeImageView>>> it = mRelatedImageViewList.entrySet().iterator(); it.hasNext();) {
					Entry<String, WeakReference<CubeImageView>> item = it.next();
					CubeImageView imageView = item.getValue().get();
					if (imageView != null) {
						handler.onLoading(this, imageView);
					}
				}
			}
		}
	}

	/**
	 * Will be called when begin load image data from dish or network
	 * 
	 * @param drawable
	 */
	public void onLoadFinish(BitmapDrawable drawable, ImageLoadHandler handler) {
		mIsLoading = false;
		mIsDoneOrAborted = true;
		if (null != handler) {
			synchronized (mRelatedImageViewList) {
				for (Iterator<Entry<String, WeakReference<CubeImageView>>> it = mRelatedImageViewList.entrySet().iterator(); it.hasNext();) {
					Entry<String, WeakReference<CubeImageView>> item = it.next();
					CubeImageView imageView = item.getValue().get();
					if (imageView != null) {
						handler.onLoadFinish(this, imageView, drawable);
					}
				}
			}
		}
	}

	public void onCancel() {
		mIsLoading = false;
		mIsDoneOrAborted = true;
	}

	public String getRemoteUrl() {
		return mUrl;
	}

	public void setOriginSize(int width, int height) {
		mOriginSize = new Point(width, height);
	}

	public Point getOriginSize() {
		return mOriginSize;
	}

	public Point getRequestSize() {
		return mRequestSize;
	}

	/**
	 * Return the key which identifies this Image Wrapper object.
	 */
	public String getIdentityKey() {
		return mIndentityKey;
	}

	protected static String genSizeKey(String key, int w, int h) {
		if (w > 0 && h != Integer.MAX_VALUE && h > 0 && h != Integer.MAX_VALUE) {
			return new StringBuilder(key).append(SIZE_SP).append(w).append(SIZE_SP).append(h).toString();
		}
		return key;
	}

	public String genFileCacheKey(String sizeTag) {
		if (TextUtils.isEmpty(sizeTag)) {
			return Encrypt.md5(mUrl);
		} else {
			return Encrypt.md5(new StringBuilder(mUrl).append(SIZE_SP).append(sizeTag).toString());
		}
	}

	public ImageReuseInfo getImageReuseInfo() {
		return mImageReuseInfo;
	}

	public boolean equals(Object object) {
		if (object != null && object instanceof ImageTask) {
			return ((ImageTask) object).getIdentityKey().equals(getIdentityKey());
		}
		return false;
	}

	private String mStr;

	@Override
	public String toString() {
		if (mStr == null) {
			mStr = String.format("%s %sx%s", mId, mRequestSize.x, mRequestSize.y);
		}
		return mStr;
	}
}