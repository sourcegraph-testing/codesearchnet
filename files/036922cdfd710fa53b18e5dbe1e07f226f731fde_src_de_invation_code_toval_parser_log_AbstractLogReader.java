package de.invation.code.toval.parser.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.invation.code.toval.file.FileReader;

public abstract class AbstractLogReader<R extends AbstractLogReadingResult> {

	protected File inputFile;
	protected R logReadingResult;
	protected Charset charset = Charset.forName("UTF-8");

	protected AbstractLogReader(File inputFile) throws Exception {
		this.inputFile = inputFile;
		this.logReadingResult = createLogReadingResult();
	}
	
	protected AbstractLogReader(File inputFile, Charset charset) throws Exception {
		this.inputFile = inputFile;
		this.logReadingResult = createLogReadingResult();
		setCharset(charset);
	}
	
	private void setCharset(Charset charset){
		this.charset = charset;
	}

	public void readFile() throws Exception {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(inputFile.getAbsolutePath(), charset).getReader());
			String line = null;
			int lineNumber = 0;
			while ((line = in.readLine()) != null) {
				lineNumber++;
				try {
					readLine(lineNumber, line);
				} catch (LineException e) {
					logReadingResult.addLineException(e);
					if (e.isCritical()) {
						throw new Exception(e);
					}
				}
			}
			reachedEndOfFile();
		} catch (IOException e) {
			throw new Exception("Exception while reading file: " + e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					throw new Exception("Exception while closing file: " + e.getMessage());
				}
			}
		}
	}

	protected void reachedEndOfFile() throws Exception {}

	protected void readLine(int lineNumber, String line) throws LineException {
		Matcher matcher = getAcceptedLinePattern().matcher(line);
		if (matcher.find()){
			processLine(lineNumber, line, matcher);
		} else {
			if(!isHeaderLine(line)){
				throw new LineException(lineNumber, line, new Exception("Cannot parse line, unexpected structure"), false);
			}
		}
	}
	
	protected abstract Pattern getAcceptedLinePattern();
	
	protected abstract void processLine(int lineNumber, String line, Matcher matcher) throws LineException;
	
	protected abstract R createLogReadingResult();
	
	protected abstract String[] getHeaderLines();
	
	private boolean isHeaderLine(String line) {
		for (String headerLine : getHeaderLines()) {
			if (headerLine.equals(line))
				return true;
		}
		return false;
	}
	
	public R getLogReadingResult(){
		return logReadingResult;
	}

}
