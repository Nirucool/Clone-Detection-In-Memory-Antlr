package cppparser;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snt.inmemantlr.GenericParser;
import org.snt.inmemantlr.exceptions.CompilationException;
import org.snt.inmemantlr.exceptions.IllegalWorkflowException;
import org.snt.inmemantlr.exceptions.ParsingException;
import org.snt.inmemantlr.listener.DefaultTreeListener;
import org.snt.inmemantlr.stream.CasedStreamProvider;
import org.snt.inmemantlr.tree.Ast;

import java.io.File;
import java.io.FileNotFoundException;


import java.util.HashMap;
import java.util.Map;

public class Grammar {

	private Grammar() {
		throw new IllegalStateException("Grammar class");
	}


	private static final Logger LOGGER = LoggerFactory.getLogger(Grammar.class);

	private static GenericParser create(File... gfile) {
		GenericParser gp = null;
		try {
			gp = new GenericParser(gfile);
		} catch (FileNotFoundException e) {
			LOGGER.error(e.getMessage());
			return null;
		}
		DefaultTreeListener t = new DefaultTreeListener();

		gp.setListener(t);

		try {
			gp.compile();
		} catch (CompilationException e) {
			LOGGER.error(e.getMessage());
			return null;
		}

		return gp;
	}

	private static boolean excludeFile(File f) {
		return (FilenameUtils.getExtension(f.getName()).equals("errors") ||
				FilenameUtils.getExtension(f.getName()).equals("tree"));
	}

	public static boolean run(File[] ok,
			String ept, File... gfile) {
		return run(GenericParser.CaseSensitiveType.NONE, ok, ept, gfile);
	}


	public static boolean run(GenericParser.CaseSensitiveType t, File[] ok,
			String ept, File... gfile) {
		GenericParser gp = create(gfile);

		if (gp == null)
			return false;

		DefaultTreeListener dt = new DefaultTreeListener();

		gp.setListener(dt);

		switch (t) {
		case UPPER:
			gp.setStreamProvider(new CasedStreamProvider(GenericParser
					.CaseSensitiveType.UPPER));
			break;
		case LOWER:
			gp.setStreamProvider(new CasedStreamProvider(GenericParser
					.CaseSensitiveType.LOWER));
			break;
		case NONE:
			break;

		}

		for (File f : ok) {

			if(excludeFile(f)) {
				LOGGER.info("skip {}", f.getAbsoluteFile());
				continue;
			}

			LOGGER.info("parse {}", f.getAbsoluteFile());
			try {
				gp.parse(f,ept, GenericParser.CaseSensitiveType.NONE);
			} catch (FileNotFoundException | IllegalWorkflowException |
					ParsingException e) {
				LOGGER.error(e.getMessage());
				return false;
			}
		}
		return true;
	}

	public static Map<File, Ast> runAndGetAsts(File[] ok, File... gfile) {
		GenericParser gp = create(gfile);

		Map<File, Ast> ret = new HashMap<>();

		for (File f : ok) {


			if(excludeFile(f)) {
				LOGGER.info("skip {}", f.getAbsoluteFile());
				continue;
			}

			LOGGER.info("parse {}", f.getAbsoluteFile());

			
			DefaultTreeListener l=null;
			if(gp!=null)
				l = (DefaultTreeListener) gp.getListener();
			if(l!=null)
				ret.put(f, l.getAst());
		}

		return ret;
	}

	public static boolean run(File... gfile) {
		GenericParser gp = create(gfile);
		return (gp != null);
	}


}



