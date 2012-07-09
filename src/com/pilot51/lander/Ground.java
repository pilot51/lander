package com.pilot51.lander;

public class Ground {
	protected static Ground current = new Ground();
	private String name;
	private int[] plot;
	
	/** Create blank instance. */
	protected Ground() {}
	
	protected Ground(String name, String plot) {
		set(name, plot);
	}
	
	protected Ground(String name, int[] plot) {
		set(name, plot);
	}
	
	protected void set(String name, String plot) {
		this.name = name;
		setPlot(plot);
	}
	
	protected void set(String name, int[] plot) {
		this.name = name;
		this.plot = plot;
	}
	
	protected String getName() {
		return name;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	protected int[] getPlot() {
		return plot;
	}
	
	protected void setPlot(int[] plot) {
		this.plot = plot;
	}
	
	protected int[] setPlot(String data) {
		if (data == null) {
			plot = null;
			return null;
		}
		String[] sMap = data.split(" ");
		plot = new int[sMap.length];
		for (int i = 0; i < sMap.length; i++) {
			plot[i] = Integer.parseInt(sMap[i]);
		}
		return plot;
	}
	
	protected String getPlotString() {
		if (plot == null) return null;
		String str = new String();
		for (int i = 0; i < plot.length; i++) {
			if (i > 0) str += " ";
			str += plot[i];
		}
		return str;
	}
	
	/** @return true if map data can be loaded, otherwise false. */
	protected boolean isValid() {
		if (plot != null && plot.length >= 2) return true;
		else return false;
	}
	
	protected void clear() {
		name = null;
		plot = null;
	}
}
