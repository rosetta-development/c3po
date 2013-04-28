package com.petpet.c3po.gatherer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.petpet.c3po.api.gatherer.MetaDataGatherer;
import com.petpet.c3po.api.model.helper.MetadataStream;
import com.petpet.c3po.common.Constants;

//TODO implement as a worker adding new streams to the queue.
// then the terminate condition will be this queue is empty (this worker is ready (some flag))
// and the other two pools await termination...
public class LocalFileGatherer extends Thread implements MetaDataGatherer {

  private static final Logger LOG = LoggerFactory.getLogger(LocalFileGatherer.class);

  private Map<String, String> config;

  private final Queue<String> queue;

  private long sum;

  private boolean ready;

  public LocalFileGatherer(Map<String, String> config) {
    this.config = config;
    this.queue = new LinkedList<String>();
    this.ready = false;
    setName("LocalFileGatherer");
  }

  @Override
  public synchronized void run() {
    String path = this.config.get(Constants.CNF_COLLECTION_LOCATION);
    boolean recursive = Boolean.valueOf(this.config.get(Constants.CNF_RECURSIVE));

    // TODO checks and throw exception if something is wrong...
    
    this.ready = false;
    this.sum = this.traverseFiles(new File(path), recursive, true);
    LOG.info("{} files were gathered successfully", this.sum);
    this.ready = true;
  }

  public MetadataStream getNext() {
    synchronized (queue) {

      MetadataStream result = null;
      String filePath = queue.poll();
      if (filePath != null) {
        try {
          result = new MetadataStream(filePath, new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException e) {
          LOG.warn("File not found: {}. {}", filePath, e.getMessage());
        }
      }

      return result;

    }
  }

  private long traverseFiles(File file, boolean recursive, boolean firstLevel) {
    long sum = 0;

    if (file.isDirectory() && (recursive || firstLevel)) {
      File[] files = file.listFiles();
      for (File f : files) {
        sum += traverseFiles(f, recursive, false);
      }
    } else {
      synchronized (queue) {
        this.queue.add(file.getAbsolutePath());
        sum++;
        this.notify();
      }
    }

    return sum;
  }

  @Override
  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  @Override
  public long getCount() {
    throw new UnsupportedOperationException("This method is deprecated and not supported anymore.");
  }

  @Override
  public long getRemaining() {
    throw new UnsupportedOperationException("This method is deprecated and not supported anymore.");
  }

  @Override
  public List<MetadataStream> getNext(int count) {
    // TODO implement me...
    return null;
  }

  @Override
  public boolean hasNext() {
    synchronized (queue) {
      return !this.queue.isEmpty();
    }
  }

  @Override
  public boolean isReady() {
    return this.ready;
  }
}
