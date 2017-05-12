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
 *   Mar 7, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.metadata.impl.DBMetadataImpl;
import org.knime.core.node.port.database.metadata.model.DBMetadata;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * DB metadata provider to provide DBMetadata.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBMetadataProvider {

    private final CredentialsProvider m_cp;

    private final DatabaseConnectionSettings m_settings;

    private DBMetadata m_metadata;

    private Timestamp m_timestamp;

    /**
     * Create a new DBMetadataProvider.
     *
     * @param cp the credentials provider
     * @param settings the connection settings
     */
    public DBMetadataProvider(final CredentialsProvider cp, final DatabaseConnectionSettings settings) {
        m_cp = cp;
        m_settings = settings;
        m_metadata = null;
    }

    /**
     * Get the metadata.
     *
     * @return the DBMetadata
     */
    public DBMetadata getMetadata() {
        return m_metadata;
    }

    /**
     * Fetch the metadata from the database.
     *
     * @throws SQLException
     * @throws InvalidSettingsException
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    public void refresh() throws SQLException, InvalidSettingsException, IOException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {
        m_metadata = new DBMetadataImpl(m_settings, m_cp);
        m_timestamp = new Timestamp(System.currentTimeMillis());
    }

    /**
     * The timestamp when the metadata is last refreshed.
     *
     * @return the timestamp
     */
    public Timestamp getTimestamp() {
        return m_timestamp;
    }

}
