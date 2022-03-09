package unisa;

public class Task {
	private String title;
	private String start;
	private String end;
	
	public Task(String name) {
		super();
		this.title = name;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public String getEnd() {
		return end;
	}
	public void setEnd(String end) {
		this.end = end;
	}
	
	@Override
	public String toString() {
		return title+": "+start+" "+end;
	}
}