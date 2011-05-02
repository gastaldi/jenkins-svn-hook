package com.george.jenkins;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNInfo {

	private static final String USER = System.getProperty("svn.user", "guest");
	private static final String PASSWORD = System.getProperty("svn.pass", "guest@host.com");

	private String svnServer;
	private long revision;

	public SVNInfo(String server, long revision) {
		this.svnServer = server;
		this.revision = revision;
	}

	/**
	 * Retorna os arquivos que mudaram na revisao
	 * 
	 * @param revision
	 * @return
	 * @throws IOException
	 * @throws SVNException
	 */
	public List<String> diffSvnKit() throws IOException, SVNException {
		DAVRepositoryFactory.setup();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER,
				PASSWORD);
		SVNDiffClient client = new SVNDiffClient(authManager, null);
		SVNURL svnUrl = SVNURL.parseURIDecoded(svnServer);
		SVNRevision pegRevision = SVNRevision.create(revision);
		final List<String> lista = new LinkedList<String>();
		client.doDiffStatus(svnUrl, SVNRevision.create(revision - 1), pegRevision, pegRevision,
				SVNDepth.UNKNOWN, false, new ISVNDiffStatusHandler() {
					public void handleDiffStatus(SVNDiffStatus diffStatus) throws SVNException {
						String url = diffStatus.getPath();
						for (String item : lista) {
							if (item.contains(url))
								return;
						}
						lista.add(url);
					}
				});
		return lista;
	}

	/**
	 * Retorna o comentário da revisão atual
	 * 
	 * @param revision
	 * @return
	 * @throws IOException
	 * @throws SVNException
	 */
	public String comentario() throws IOException, SVNException {
		DAVRepositoryFactory.setup();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER,
				PASSWORD);
		SVNURL svnUrl = SVNURL.parseURIDecoded(svnServer);
		SVNRepository repository = SVNRepositoryFactory.create(svnUrl);
		repository.setAuthenticationManager(authManager);
		String comentario = repository.getRevisionPropertyValue(revision, SVNRevisionProperty.LOG)
				.getString();
		repository.closeSession();
		return comentario;
	}
}
