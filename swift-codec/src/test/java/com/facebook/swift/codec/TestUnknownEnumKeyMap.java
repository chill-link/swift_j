package com.facebook.swift.codec;

import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.coercion.DefaultJavaCoercions;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.reflect.TypeToken;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Tests that maps with unknown enum keys are properly skipped without corrupting
 * subsequent entries during decoding.
 */
public class TestUnknownEnumKeyMap
{
    private ThriftCodecManager codecManager;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        codecManager = new ThriftCodecManager();
        codecManager.getCatalog().addDefaultCoercions(DefaultJavaCoercions.class);

        ThriftCatalog catalog = codecManager.getCatalog();
        ThriftType fruitType = catalog.getThriftType(Fruit.class);
        codecManager.addCodec(new EnumThriftCodec<>(fruitType));
    }

    @Test
    public void testSkipUnknownEnumKey()
            throws Exception
    {
        TMemoryBuffer transport = new TMemoryBuffer(64);
        TProtocol protocol = new TBinaryProtocol(transport);

        protocol.writeMapBegin(new TMap(TType.I32, TType.STRING, 2));
        protocol.writeI32(123); // unknown enum key
        protocol.writeString("bad");
        protocol.writeI32(Fruit.BANANA.ordinal());
        protocol.writeString("banana");
        protocol.writeMapEnd();

        // reset protocol for reading
        protocol = new TBinaryProtocol(transport);

        ThriftCodec<Map<Fruit, String>> mapCodec =
                codecManager.getCodec(new TypeToken<Map<Fruit, String>>() {});

        Map<Fruit, String> result = mapCodec.read(protocol);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(Fruit.BANANA), "banana");
    }
}

