/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.framework.detection.impl;

import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FrameworkDetectionContext;
import com.intellij.framework.detection.FrameworkDetector;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class FrameworkDetectionProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.framework.detection.impl.FrameworkDetectionProcessor");
  private final ProgressIndicator myProgressIndicator;
  private final MultiMap<FileType, FrameworkDetectorData> myDetectorsByFileType;
  private Set<VirtualFile> myProcessedFiles;

  private final FrameworkDetectionContext myContext;

  public FrameworkDetectionProcessor(ProgressIndicator progressIndicator, final FrameworkDetectionContext context) {
    myProgressIndicator = progressIndicator;
    final FrameworkDetector[] detectors = FrameworkDetector.EP_NAME.getExtensions();
    myDetectorsByFileType = new MultiMap<FileType, FrameworkDetectorData>();
    for (FrameworkDetector detector : detectors) {
      myDetectorsByFileType.putValue(detector.getFileType(), new FrameworkDetectorData(detector));
    }
    myContext = context;
  }

  public List<DetectedFrameworkDescription> processRoots(List<File> roots) {
    myProcessedFiles = new HashSet<VirtualFile>();
    for (File root : roots) {
      VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(root);
      if (virtualFile == null) continue;
      collectSuitableFiles(virtualFile);
    }
    List<DetectedFrameworkDescription> result = new ArrayList<DetectedFrameworkDescription>();
    for (FrameworkDetectorData data : myDetectorsByFileType.values()) {
      result.addAll(data.myDetector.detect(data.mySuitableFiles, myContext));
    }
    return result;
  }

  private void collectSuitableFiles(@NotNull VirtualFile file) {
    if (myProgressIndicator.isCanceled() || !myProcessedFiles.add(file)) return;

    if (file.isDirectory()) {
      file.getChildren();//initialize myChildren field to ensure that refresh will be really performed
      file.refresh(false, false);
      VirtualFile[] children = file.getChildren();
      for (VirtualFile child : children) {
        collectSuitableFiles(child);
      }
      return;
    }

    final FileType fileType = file.getFileType();
    if (!myDetectorsByFileType.containsKey(fileType)) {
      return;
    }

    myProgressIndicator.setText2(file.getPresentableUrl());
    try {
      FileContent fileContent = new FileContent(file, file.contentsToByteArray(false));
      for (FrameworkDetectorData detector : myDetectorsByFileType.get(fileType)) {
        if (detector.myFilePattern.accepts(fileContent)) {
          detector.mySuitableFiles.add(file);
        }
      }
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  private static class FrameworkDetectorData {
    private FrameworkDetector myDetector;
    private final ElementPattern<FileContent> myFilePattern;
    private final List<VirtualFile> mySuitableFiles = new ArrayList<VirtualFile>();

    public FrameworkDetectorData(FrameworkDetector detector) {
      myDetector = detector;
      myFilePattern = detector.createSuitableFilePattern();
    }
  }
}
