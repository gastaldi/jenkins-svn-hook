package com.george.jenkins;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post Commit Hook no SVN para chamar o hudson Baseado em
 * http://www.jroller.com/mcfoggy/entry/svn_hook_scripts_with_groovy1
 * 
 * @author george
 * 
 */
public class JenkinsPostCommitHook {

	private static final String BRANCHES_PATH = "branches";
	private static final boolean DRY_RUN = Boolean.getBoolean("dryRun");
	private static final boolean DEBUG_MODE = Boolean.getBoolean("debug");

	private static final String DEPLOY_TOKEN = "@deploy";
	private static final String TASK_TOKEN = "@task";
	
	private static final Pattern compiledExp = Pattern.compile(System
			.getProperty("proj.regex", "(.*)/(trunk|branches)/(.*)"));

	private String jenkinsURL = System.getProperty("jenkins.url",
			"http://ci.jenkins-ci.org/");
	private String token = System.getProperty("token", "JK");

	// svn://jserv/svnrepos
	private SVNInfo svnInfo;
	
	public JenkinsPostCommitHook(String svnServer, long revision) {
		svnInfo = new SVNInfo(svnServer,revision);
	}

	public static void main(String[] args) {
		try {
			if (args == null || args.length != 2) {
				throw new RuntimeException(
						"Args Length should be 2 (SVNServer, revision) and it is not!");
			}
			JenkinsPostCommitHook hook = new JenkinsPostCommitHook(args[0], Long
					.parseLong(args[1]));
			hook.notifyHudson();
		} catch (Exception e) {
			System.err.println("Excecao: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Notifica Hudson
	 * 
	 * @throws Exception
	 */
	public void notifyHudson() throws Exception {
		String[] projs = extraiNomeProjetosPorRevisao();
		String comentario = extrairComentario();
		if (DEBUG_MODE || DRY_RUN)
			System.out.println("Notifying projects: " + Arrays.toString(projs));
		if (!DRY_RUN)
			for (String projeto : projs) {
				//Executa Build
				build(projeto, comentario);
				if (comentario.contains(DEPLOY_TOKEN)) {
					//Deve esperar o build anterior para fazer o deploy
					Thread.sleep(10000);
					deploy(projeto);
				}
				if (comentario.contains(TASK_TOKEN)) {
					//Deve esperar o build anterior para fazer o deploy
					Thread.sleep(10000);
					executeTask(projeto,comentario);
				}
			}
	}

	/**
	 * Retorna um array com o nome dos projetos a serem notificados Toma como
	 * base o svn diff do subversion
	 * 
	 * @param revision
	 * @return
	 * @throws Exception
	 */
	String[] extraiNomeProjetosPorRevisao() throws Exception {
		HashSet<String> set = new HashSet<String>();
		List<String> linhas = svnInfo.diffSvnKit();
		for (String linha : linhas) {
			String nomeProj = extrairNomeProjeto(linha);
			if (nomeProj != null) {
				set.add(nomeProj);
			}
		}
		return set.toArray(new String[0]);
	}
	
	/**
	 * Extrai o comentário da revisão atual
	 * @return
	 * @throws Exception
	 */
	String extrairComentario() throws Exception {
		return svnInfo.comentario();
	}


	/**
	 * Extrai o nome do projeto
	 * 
	 * @param caminho
	 * @return
	 */
	static String extrairNomeProjeto(String caminho) {
		String projeto = null;
		Matcher matcher = compiledExp.matcher(caminho);
		if (matcher.find()) {
			//INFRAWEB-37
			String tipo = matcher.group(2);
			if (tipo.equals(BRANCHES_PATH)) {
				projeto = matcher.group(3).toUpperCase();
				int indexOf = projeto.indexOf('/');
				if (indexOf > -1) {
					projeto = projeto.substring(0, indexOf);
				}
			} else {
				projeto = matcher.group(1).toUpperCase();
				int indexOf = projeto.lastIndexOf('/');
				if (indexOf > -1) {
					projeto = projeto.substring(indexOf + 1);
				}
			}
		}
		return projeto;
	}
	
	
	/**
	 * @param projeto
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void build(String projeto, String comentario) throws IOException {
		String urlBuild = montaURLBuild(projeto, comentario);
		doGet(urlBuild);
	}
	
	/**
	 * Executa uma determinada task
	 * @param projeto
	 * @param comentario
	 */
	private void executeTask(String projeto, String comentario) throws IOException {
		String urlTask = montaURLTask(projeto, comentario);
		doGet(urlTask);
	}
	

	/**
	 * @param projeto
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void deploy(String projeto) throws IOException {
		String urlBuild = montaURLDeploy(projeto);
		doGet(urlBuild);
	}
	

	private void doGet(String urlBuild) throws IOException {
		URL u = new URL(urlBuild);
		try {
			// Faz um GET
			u.openStream().close();
		} catch (FileNotFoundException fnf) {
			// URL nao existe. Ignora
		}
	}

	/**
	 * Monta URL da build
	 * 
	 * @param projeto
	 * @return
	 */
	private String montaURLBuild(String projeto, String comentario) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(jenkinsURL);
		sb.append("/job/").append(projeto).append("/build");
		sb.append("?token=").append(token);
		sb.append("&cause=").append(URLEncoder.encode(comentario,"UTF-8"));
		return sb.toString();
	}


	/**
	 * Monta URL da build
	 * 
	 * @param projeto
	 * @return
	 */
	private String montaURLDeploy(String projeto) {
		StringBuilder sb = new StringBuilder(jenkinsURL);
		sb.append("/job/").append(projeto).append("/batchTasks/task/Deploy/execute");
		return sb.toString();
	}

	/**
	 * Monta URL da build
	 * 
	 * @param projeto
	 * @return
	 */
	String montaURLTask(String projeto, String comentario) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(jenkinsURL);
		int idxStart = comentario.indexOf(TASK_TOKEN) + TASK_TOKEN.length();
		int idxEnd = comentario.indexOf("@",idxStart);
		String task = comentario.substring(idxStart,idxEnd).trim();
		String encodedTask = task.replace(" ", "%20");
		sb.append("/job/").append(projeto).append("/batchTasks/task/").append(encodedTask).append("/execute");
		return sb.toString();
	}
	
}
