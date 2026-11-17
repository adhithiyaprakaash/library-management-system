package com.college.library.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Template-method base class: subclasses only describe how one record maps to one text line. */
public abstract class CsvRepository<T> implements Repository<T, String> {
    protected static final String SEP = "|";

    private final Path file;
    private final Map<String, T> records = new LinkedHashMap<>();
    private int skippedLines;

    protected CsvRepository(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        load();
    }

    protected abstract T fromFields(String[] fields);   // may throw RuntimeException for a bad line
    protected abstract String[] toFields(T entity);
    protected abstract String idOf(T entity);

    private void load() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) return;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    T entity = fromFields(line.split("\\|", -1));
                    records.put(idOf(entity), entity);
                } catch (RuntimeException badLine) {
                    skippedLines++;          // a damaged line must not stop the whole application
                }
            }
        }
    }

    private void flush() throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (T entity : records.values()) {
                out.write(String.join(SEP, toFields(entity)));
                out.newLine();
            }
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override public List<T> findAll() { return new ArrayList<>(records.values()); }
    @Override public Optional<T> findById(String id) { return Optional.ofNullable(records.get(id)); }
    @Override public boolean existsById(String id) { return records.containsKey(id); }
    public int getSkippedLines() { return skippedLines; }

    @Override
    public void save(T entity) throws IOException {
        T previous = records.put(idOf(entity), entity);
        try { flush(); }
        catch (IOException e) {
            if (previous == null) records.remove(idOf(entity)); else records.put(idOf(entity), previous);
            throw e;
        }
    }

    @Override
    public boolean deleteById(String id) throws IOException {
        T removed = records.remove(id);
        if (removed == null) return false;
        try { flush(); }
        catch (IOException e) { records.put(id, removed); throw e; }
        return true;
    }
}
