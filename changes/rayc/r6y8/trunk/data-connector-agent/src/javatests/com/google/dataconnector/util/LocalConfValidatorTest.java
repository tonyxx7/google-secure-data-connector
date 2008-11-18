/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.util;

import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.util.LocalConfValidator.FileFactory;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.File;

/**
 * Tests for the {@link LocalConfValidator} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class LocalConfValidatorTest extends TestCase {
  
  private LocalConf localConf;
  private LocalConfValidator localConfValidator;
  private FileFactory mockFileFactory;
  private File mockGoodFile;
  private File mockBadFile;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FakeLocalConfGenerator fakeLocalConfGenerator = new FakeLocalConfGenerator();
    localConf = fakeLocalConfGenerator.getFakeLocalConf();
    
    mockFileFactory = EasyMock.createMock(LocalConfValidator.FileFactory.class);
    localConfValidator = new LocalConfValidator(mockFileFactory);
    
    mockGoodFile = EasyMock.createMock(File.class);
    EasyMock.expect(mockGoodFile.canRead()).andReturn(true).anyTimes();
    EasyMock.replay(mockGoodFile);
    
    mockBadFile = EasyMock.createMock(File.class);
    EasyMock.expect(mockBadFile.canRead()).andReturn(false);
    EasyMock.replay(mockBadFile);
    
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.RULES_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSL_KEY_STORE_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSHD)).andReturn(mockGoodFile);
    EasyMock.replay(mockFileFactory); 
  }
  
  @Override
  protected void tearDown() throws Exception {
    localConf = null;
    localConfValidator = null;
    mockBadFile = null;
    mockGoodFile = null;
    mockFileFactory = null;
    super.tearDown();
  }
  
  public void testProperConfigResourceRules() throws LocalConfException {
    localConfValidator.validate(localConf);
    EasyMock.verify(mockGoodFile);
    EasyMock.verify(mockFileFactory);
  }
  
  // RulesFile
  public void testBadRulesFile() {
    String badFile = "/bad/file";
    localConf.setRulesFile(badFile);
    
    // Overwrite success case
    mockFileFactory = EasyMock.createMock(LocalConfValidator.FileFactory.class);
    EasyMock.expect(mockFileFactory.getFile(badFile)).andReturn(mockBadFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSL_KEY_STORE_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSHD)).andReturn(mockGoodFile);
    EasyMock.replay(mockFileFactory);
    
    // Create new validator with our updated factory.
    localConfValidator = new LocalConfValidator(mockFileFactory);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("Cannot read"));
      EasyMock.verify(mockBadFile);
      EasyMock.verify(mockGoodFile);
      EasyMock.verify(mockFileFactory);
      return;
    }
    fail("did not get LocalConfException");
  }
  
  // Socks Server Port
  public void testBadSdcServerPort() {
    localConf.setSdcServerPort(3242343);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("invalid 'sdcServerPort'"));
      return;
    }
    fail("did not get LocalConf");
  }
  
  // Domain
  public void testBadDomain() {
    localConf.setDomain("asdfasdf");
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("not valid"));
      return;
    }
    fail("did not get LocalConf");
  }
  
  public void testBadDomainInvalidTld() {
    localConf.setDomain("asdfasdf.asdfasfdafdsfadsf");
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("not valid"));
      return;
    }
    fail("did not get LocalConf");
  }
  
  
  // SslKeyStoreFile
  public void testBadSslKeystoreFile() {
    String badFile = "/bad/file";
    localConf.setSslKeyStoreFile(badFile);
    
    // Overwrite success case
    mockFileFactory = EasyMock.createMock(LocalConfValidator.FileFactory.class);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.RULES_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(badFile)).andReturn(mockBadFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSHD)).andReturn(mockGoodFile);
    EasyMock.replay(mockFileFactory);
    
    // Create new validator with our updated factory.
    localConfValidator = new LocalConfValidator(mockFileFactory);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("Cannot read"));
      EasyMock.verify(mockBadFile);
      EasyMock.verify(mockGoodFile);
      EasyMock.verify(mockFileFactory);
      return;
    }
    fail("did not get LocalConfException");
  }
  
  // Socks Server Port
  public void testBadStartingHttpProxyPort() {
    localConf.setStartingHttpProxyPort(3242343);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("invalid 'startingHttpProxyPort'"));
      return;
    }
    fail("did not get LocalConf");
  }
  
  // Socks Server Port
  public void testBadSocksServerPort() {
    localConf.setSocksServerPort(3242343);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("invalid 'socks"));
      return;
    }
    fail("did not get LocalConf");
  }
  
  // Sshd
  public void testBadSshd() {
    String badFile = "/bad/file";
    localConf.setSshd(badFile);
    
    // Overwrite success case
    mockFileFactory = EasyMock.createMock(LocalConfValidator.FileFactory.class);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.RULES_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(FakeLocalConfGenerator.SSL_KEY_STORE_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileFactory.getFile(badFile)).andReturn(mockBadFile);
    EasyMock.replay(mockFileFactory);
    
    // Create new validator with our updated factory.
    localConfValidator = new LocalConfValidator(mockFileFactory);
    
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("Cannot read"));
      EasyMock.verify(mockBadFile);
      EasyMock.verify(mockGoodFile);
      EasyMock.verify(mockFileFactory);
      return;
    }
    fail("did not get LocalConfException");
  }
}
