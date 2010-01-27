package geo.kml;

import genj.geo.GeoLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

public class CompactPlacemarkWriter extends PlacemarkWriter {

	public CompactPlacemarkWriter(Writer out, String idFormat) {
		super(out, 1, idFormat);
	}
	protected void writePlacemarkContent(String indent, GeoLocation location, String idFormat)
			throws IOException {
		Names names = new Names(location);
		Iterator<String> it = names.keys();
		while (it.hasNext()) {
			String name = it.next();
			out.write(indent + "\t" + name + " (" + names.getValue(name)
					+ ")<br>\n ");
		}
	}
}
