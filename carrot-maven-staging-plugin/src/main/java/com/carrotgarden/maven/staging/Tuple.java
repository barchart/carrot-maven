package com.carrotgarden.maven.staging;

import java.util.ArrayList;
import java.util.List;

class Tuple {

	final String classifier;
	final String extension;

	Tuple(final String classifier, final String type) {
		this.classifier = classifier;
		this.extension = type;
	}

	static Tuple from(final String term) {
		if (term.contains(":")) {
			final String[] split = term.split(":");
			return new Tuple(split[0], split[1]);
		} else {
			return new Tuple(null, term);
		}
	}

	static List<Tuple> fromList(final String textList) {
		final List<Tuple> tupleList = new ArrayList<Tuple>();
		final String[] split = textList.split(",");
		for (final String term : split) {
			tupleList.add(from(term));
		}
		return tupleList;
	}

}