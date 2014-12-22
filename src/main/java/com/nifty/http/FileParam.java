package com.nifty.http;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by BaoRui on 2014/12/19.
 */
public class FileParam {

	public final static String DEFAULT_TYPE = "application/octet-stream";

	public String key;
	public String filename;
	public String fileType = "";
	public File file;

	public FileParam(String key, File file) {
		this(key, file, DEFAULT_TYPE);
	}

	public FileParam(String key, File file, String fileType) {
		this(key, null, file, fileType);
	}

	public FileParam(String key, String filename, File file, String fileType) {
		if (TextUtils.isEmpty(filename)) {
			filename = file.getName();
		}
		this.key = key;
		this.filename = filename;
		this.file = file;
		this.fileType = fileType;
	}
}
