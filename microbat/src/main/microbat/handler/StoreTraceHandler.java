package microbat.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.Settings;
import microbat.views.MicroBatViews;

public class StoreTraceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Trace trace = MicroBatViews.getTraceView().getTrace();
		Connection conn = null;
		List<Statement> stmts = new ArrayList<Statement>();
		ResultSet rs = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			String traceId = insertTrace(trace, conn, stmts);
			insertSteps(traceId, trace.getExectionList(), conn, stmts);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			closeDb(conn, stmts, rs);
		}
		
		System.out.println("test");
		return null;
	}

	private String insertTrace(Trace trace, Connection conn, List<Statement> stmts)
			throws SQLException {
		PreparedStatement ps;
		String sql = "insert into trace (launch_class, project_name, project_version, bug_id, generated_time) "
				+ "values (?, ?, ?, ?, ?)";
		ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		int idx = 1;
		ps.setString(idx++, trace.getAppJavaClassPath().getLaunchClass());
		ps.setString(idx++, Settings.projectName);
		ps.setString(idx++, null);
		ps.setString(idx++, null);
		ps.setTimestamp(idx++, new Timestamp(System.currentTimeMillis()));
		stmts.add(ps);
		ps.execute();
		try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
			if (generatedKeys.next()) {
				return generatedKeys.getString(1);
			} else {
				throw new SQLException("Update failed, no ID obtained.");
			}
		}
	}
	
	private void insertSteps(String traceId, List<TraceNode> exectionList, Connection conn,
			List<Statement> stmts) throws SQLException {
		String sql = "INSERT INTO step (trace_id, step_order, control_dominator, step_in, step_over, invocation_parent, loop_parent,"
				+ "location_id) VALUES (?,?,?,?,?,?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		Set<BreakPoint> locations = new HashSet<>();
		for (int i = 0; i < exectionList.size(); i++) {
			TraceNode node = exectionList.get(i);
			int idx = 1;
			ps.setString(idx++, traceId);
			ps.setInt(idx++, node.getOrder());
			setNodeOrder(ps, idx++, node.getControlDominator());
			setNodeOrder(ps, idx++, node.getStepInNext());
			setNodeOrder(ps, idx++, node.getStepOverNext());
			setNodeOrder(ps, idx++, node.getInvocationParent());
			setNodeOrder(ps, idx++, node.getLoopParent());
			ps.setString(idx++, node.getBreakPoint().getId());
			ps.addBatch();
			locations.add(node.getBreakPoint());
		}
		ps.executeBatch();
		stmts.add(ps);
		insertLocation(traceId, locations, conn, stmts);
		insertStepVariableRelation(traceId, exectionList, conn, stmts);
	}

	private void insertStepVariableRelation(String traceId, List<TraceNode> exectionList, Connection conn,
			List<Statement> stmts) throws SQLException {
		String sql = "INSERT INTO stepVariableRelation (var_id, trace_id, step_order, rw) VALUES (?, ?, ?, ?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		for (TraceNode node : exectionList) {
			for (VarValue varVal : node.getReadVariables()) {
				int idx = 1;
				ps.setString(idx++, varVal.getVarID());
				ps.setString(idx++, traceId);
				ps.setInt(idx++, node.getOrder());
				ps.setInt(idx++, 1);
				ps.addBatch();
			}
			for (VarValue varVal : node.getReadVariables()) {
				int idx = 1;
				ps.setString(idx++, varVal.getVarID());
				ps.setString(idx++, traceId);
				ps.setInt(idx++, node.getOrder());
				ps.setInt(idx++, 2);
				ps.addBatch();
			}
		}
		ps.executeBatch();
		stmts.add(ps);
	}

	private void setNodeOrder(PreparedStatement ps, int idx, TraceNode node) throws SQLException {
		if (node != null) {
			ps.setInt(idx, node.getOrder());
		} else {
			ps.setNull(idx, java.sql.Types.INTEGER);
		}
	}
	
	private void insertLocation(String traceId, Set<BreakPoint> locations, Connection conn, List<Statement> stmts)
			throws SQLException {
		String sql = "INSERT INTO location (trace_id, location_id, class_name, line_number, is_conditional, is_return) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		for (BreakPoint location : locations) {
			int idx = 1;
			ps.setString(idx++, traceId);
			ps.setString(idx++, location.getId());
			ps.setString(idx++, location.getDeclaringCompilationUnitName());
			ps.setInt(idx++, location.getLineNumber());
			ps.setBoolean(idx++, location.isConditional());
			ps.setBoolean(idx++, location.isReturnStatement());
			ps.addBatch();
		}
		ps.executeBatch();
		stmts.add(ps);
	}
	
	private Connection getConnection() throws SQLException {
		MysqlDataSource dataSource = new MysqlDataSource();
		DBSettings settings = new DBSettings();
		dataSource.setServerName(settings.dbAddress);
		dataSource.setPort(settings.dbPort);
		dataSource.setUser(settings.username);
		dataSource.setPassword(settings.password);
		dataSource.setDatabaseName(settings.dbName);
		Connection conn = dataSource.getConnection();
		return conn;
	}

	protected void closeDb(Connection connection, List<Statement> stmts, ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		for (Statement stmt : stmts) {
			try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}