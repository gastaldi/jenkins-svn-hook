package com.george.jenkins;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.george.jenkins.JenkinsPostCommitHook;

public class JenkinsPostCommitHookTest {

	JenkinsPostCommitHook hook;
	
	@Before
	public void setUp() throws Exception {
		hook = new JenkinsPostCommitHook("https://repos.trt12.jus.br",2554);
	}
	
	@Test
	public void testProject() throws Exception {
		String[] projs = hook.extraiNomeProjetosPorRevisao();
		assertArrayEquals(new String[]{"STDI-ELO"}, projs);
	}
	

	@Test
	public void testTaskExec() throws Exception {
		String[] projs = hook.extraiNomeProjetosPorRevisao();
		assertArrayEquals(new String[]{"STDI-ELO"}, projs);
	}
	
	@Test
	public void testProjectName() throws Exception {
		String input = "A      http://www.elosoft.com.br/repos/seint/provid/Defini‡Æo/aarh/trunk/Manter containner.rtf";
		String output = JenkinsPostCommitHook.extrairNomeProjeto(input);
		assertEquals("AARH", output);
	}

	@Test
	public void testNoProjectName() throws Exception {
		String input = "projetos/trunk";
		String output = JenkinsPostCommitHook.extrairNomeProjeto(input);
		assertNull(output);
	}
	
	@Test
	public void testBranch() throws Exception {
		String input = "A      http://www.elosoft.com.br/repos/seint/provid/Defini‡Æo/aarh/branches/IssoEhUmaBranch/Manter containner.rtf";
		String output = JenkinsPostCommitHook.extrairNomeProjeto(input);
		assertEquals("ISSOEHUMABRANCH", output);
	}
	

	@Test
	public void testTask() throws Exception {
		String output = hook.montaURLTask("AARH", "Teste @task Faz Algo@ @deploy");
		assertEquals("http://www.trt12.jus.br/hudson/job/AARH/batchTasks/task/Faz%20Algo/execute", output);
	}
}
