package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

/**
 * A simple class for reading Thrift objects (of a single type) from a file.
 *
 * @author Joel Meyer
 */
public class ThriftReader {
	/**
	 * Thrift deserializes by taking an existing object and populating it. ThriftReader
	 * needs a way of obtaining instances of the class to be populated and this interface
	 * defines the mechanism by which a client provides these instances.
	 */
	public static interface TBaseCreator {
		TBase create();
	}

	/** File containing the objects. */
	protected final File file;

	/** Used to create empty objects that will be initialized with values from the file. */
	protected final TBaseCreator creator;

	/** For reading the file. */
	private BufferedInputStream bufferedIn;

	/** For reading the binary thrift objects. */
	private TBinaryProtocol binaryIn;

	/**
	 * Constructor.
	 */
	public ThriftReader(File file, TBaseCreator creator) {
		this.file = file;
		this.creator = creator;
	}

	/**
	 * Opens the file for reading. Must be called before {@link read()}.
	 */
	public void open() throws FileNotFoundException {
		bufferedIn = new BufferedInputStream(new FileInputStream(file), 2048);
		binaryIn = new TBinaryProtocol(new TIOStreamTransport(bufferedIn));
	}

	/**
	 * Checks if another objects is available by attempting to read another byte from the stream.
	 */
	public boolean hasNext() throws IOException {
		bufferedIn.mark(1);
		int val = bufferedIn.read();
		bufferedIn.reset();
		return val != -1;
	}

	/**
	 * Reads the next object from the file.
	 */
	public TBase read() throws IOException {
		TBase t = creator.create();
		try {
			t.read(binaryIn);
		} catch (TException e) {
			throw new IOException(e);
		}
		return t;
	}

	/**
	 * Close the file.
	 */
	public ThriftReader close() throws IOException {
		bufferedIn.close();
		return this;
	}
}