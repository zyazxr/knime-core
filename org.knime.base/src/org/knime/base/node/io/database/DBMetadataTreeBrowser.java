/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 15, 2017 (adewi): created
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.metadata.DBColumn;
import org.knime.core.node.port.database.metadata.DBColumnContainer;
import org.knime.core.node.port.database.metadata.DBMetadata;
import org.knime.core.node.port.database.metadata.DBSchema;
import org.knime.core.node.port.database.metadata.DBView;

/**
 *
 * @author adewi
 * @since 3.4
 */
public class DBMetadataTreeBrowser extends JPanel implements TreeSelectionListener {


    private final JTree m_tree;

    private DatabaseMetaData m_meta;

    private final DefaultMutableTreeNode m_root = new DefaultMutableTreeNode("ROOT");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBTreeBrowser.class);

    private DBMetadata m_dbmeta;

    /**
     * Create a new database browser.
     *
     * @param editor to which table and table column names are added
     */
    public DBMetadataTreeBrowser(final JEditorPane editor) {
        super(new BorderLayout());
        m_tree = new JTree(m_root);
        m_tree.setRootVisible(false);
        m_tree.setToggleClickCount(1);
        m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_tree.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent me) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                final String nodeInfo = node.toString();
                if (node.getLevel() < 2) {
                    return;
                }
                if (me.getClickCount() == 2) {
                    if (node.getLevel() == 3) { // column name
                        TreeNode tableNode = node.getParent();
                        String insert = tableNode.toString() + "." + nodeInfo;
                        if (nodeInfo.contains(" ")) {
                            insert = "\"" + insert + "\"";
                        }
                        editor.replaceSelection(insert);
                    } else { // table name
                        editor.replaceSelection(nodeInfo);
                    }
                    editor.requestFocus();
                }
            }
        });
        m_tree.addTreeSelectionListener(this);
        final JScrollPane jsp = new JScrollPane(m_tree);
        super.add(jsp, BorderLayout.CENTER);

    }

    /**
     * Update this tree and metadata from database.
     *
     * @param meta <code>DatabaseMetaData</code> used to retrieve table names and column names from.
     * @since 2.10
     */
    public final synchronized void update(final DatabaseMetaData meta, final DBMetadata con) {
        m_dbmeta = con;
        m_meta = meta;
        m_tree.collapsePath(new TreePath(m_root));
        m_root.removeAllChildren();
        ((DefaultTreeModel)m_tree.getModel()).reload(m_root);

        if (m_meta != null && m_dbmeta != null) {

            Iterable<DBSchema> schemas = con.getSchemas();

            final DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode("SCHEMA");
            schemaNode.setAllowsChildren(true);

            for (final DBSchema schema : schemas) {

                DefaultMutableTreeNode schemaName = new DefaultMutableTreeNode(schema.getName());
                schemaName.setAllowsChildren(true);
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode("TABLE");
                tableNode.setAllowsChildren(true);
                DefaultMutableTreeNode viewNode = new DefaultMutableTreeNode("VIEW");
                viewNode.setAllowsChildren(true);

                for (final DBColumnContainer table : schema.getTables()) {
                    DefaultMutableTreeNode tableName = new DefaultMutableTreeNode(table.getName());
                    tableName.setAllowsChildren(true);
                    if (table instanceof DBView) {
                        viewNode.add(tableName);
                    } else {
                        tableNode.add(tableName);
                    }
                }
                schemaName.add(tableNode);
                schemaName.add(viewNode);

                schemaNode.add(schemaName);
            }

            m_root.add(schemaNode);

        }
        m_tree.expandPath(new TreePath(m_root));
        m_tree.validate();
        m_tree.repaint();
    }

    /** {@inheritDoc} */
    @Override
    public void valueChanged(final TreeSelectionEvent event) {
        if (m_meta == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }
        final String nodeInfo = node.toString();
        try {
            String[] columnNames = getColumnNames(m_meta.getConnection(), nodeInfo);
            for (String colName : columnNames) {
                final DefaultMutableTreeNode child = new DefaultMutableTreeNode(colName);
                child.setAllowsChildren(false);
                node.add(child);
            }
        } catch (SQLException sqle) {
            LOGGER.debug(sqle);
        }
    }

    private String[] getTableNames(final String type) throws SQLException {
        final ArrayList<String> tableNames = new ArrayList<String>();
        try (ResultSet rs = m_meta.getTables(null, null, "%", new String[]{type})) {
            while (rs.next()) {
                final String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        }
        final String[] array = tableNames.toArray(new String[tableNames.size()]);
        Arrays.sort(array);
        return array;
    }

    private String[] getColumnNames(final Connection con, final String tableName) throws SQLException {
        final ArrayList<String> columnNames = new ArrayList<String>();

        for (final DBSchema schema : m_dbmeta.getSchemas()) {
            for (final DBColumnContainer table : schema.getTables()) {
                if (table.getName().equals(tableName)) {
                    for (final DBColumn column : table.getColumns()) {
                        columnNames.add(column.getName());
                    }
                }
            }
        }

        return columnNames.toArray(new String[columnNames.size()]);
    }
}
