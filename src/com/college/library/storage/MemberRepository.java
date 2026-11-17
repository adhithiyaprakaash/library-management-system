package com.college.library.storage;

import com.college.library.model.Member;
import com.college.library.model.MemberType;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

public class MemberRepository extends CsvRepository<Member> {
    public MemberRepository(Path file) throws IOException { super(file); }

    @Override protected String idOf(Member m) { return m.getId(); }

    @Override protected String[] toFields(Member m) {
        return new String[] { m.getId(), m.getName(), m.getEmail(), m.getType().name(), m.getJoinedOn().toString() };
    }

    @Override protected Member fromFields(String[] f) {
        return new Member(f[0], f[1], f[2], MemberType.valueOf(f[3]), LocalDate.parse(f[4]));
    }
}
