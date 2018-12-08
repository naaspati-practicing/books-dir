import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import sam.books.PathsImpl;

public class PathsImpl2 extends PathsImpl {
	public final String markerLowerCased;
	
	public PathsImpl2(ResultSet rs) throws SQLException {
		super(rs);
		markerLowerCased = (getMarker() == null ? new File(getPath()).getName() : getMarker()).toLowerCase();
	}

	public PathsImpl2(DataInputStream is) throws IOException {
		super(is.readInt(), is.readUTF(), is.readUTF());
		markerLowerCased = is.readUTF();
	}

	public void write(DataOutputStream os) throws IOException {
		os.writeInt(getPathId());
		os.writeUTF(getPath());
		os.writeUTF(getMarker());
		os.writeUTF(markerLowerCased);
	}
}
