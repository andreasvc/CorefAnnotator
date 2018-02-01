package de.unistuttgart.ims.coref.annotator;

public class PotentialAnnotation {
	int begin;
	int end;
	CasTextView textView;

	public PotentialAnnotation(CasTextView textView, int begin, int end) {
		this.begin = begin;
		this.end = end;
		this.textView = textView;
	}

	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public CasTextView getTextView() {
		return textView;
	}

	public void setTextView(CasTextView textView) {
		this.textView = textView;
	}
}