/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.browser;

import java.util.ArrayList;
import java.util.List;

public class BrowserNode {
	
	public enum BrowserNodeType {
		ESDL_FILE("file-esdl"),
		FOLDER("folder"),
		BINARY("file"),
		TEXT("file-text"),
		OTHER("file");
		
		private String iconName;
		BrowserNodeType(String iconName) {
			this.iconName = iconName;
		}
		
		public String getIconName() {
			return this.iconName;
		}
	}
	
	private String name;
	private BrowserNodeType type;
	private String path;
	private List<BrowserNode> children = new ArrayList<>();
	private boolean hasSubNodes = false;
	private boolean writable;

	public BrowserNode(String name, String path) {
		this.name = name;
		this.path = path;
		this.type = BrowserNodeType.OTHER;
	}
	
	@Override
	public String toString() {
		return "BrowserNode [name=" + name + ", type=" + type + ", path=" + path + ", children=" + children
				+ ", hasSubNodes=" + hasSubNodes + ", writable=" + writable + "]";
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public BrowserNodeType getType() {
		return type;
	}
	public void setType(BrowserNodeType type) {
		this.type = type;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public List<BrowserNode> getChildren() {
		return children;
	}
	public void setChildren(List<BrowserNode> children) {
		this.children = children;
	}

	public boolean hasSubNodes() {
		return hasSubNodes;
	}

	public void setHasSubNodes(boolean hasSubNodes) {
		this.hasSubNodes = hasSubNodes;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	public boolean isWritable() {
		return writable;
	}
}
