package microbat.instrumentation.cfgcoverage.graph;

import java.util.List;

public class CoveragePath {
	private List<Integer> coveredTcs;
	private List<Integer> path;

	public List<Integer> getCoveredTcs() {
		return coveredTcs;
	}

	public void setCoveredTcs(List<Integer> coveredTcs) {
		this.coveredTcs = coveredTcs;
	}

	public List<Integer> getPath() {
		return path;
	}

	public void setPath(List<Integer> path) {
		this.path = path;
	}
}
