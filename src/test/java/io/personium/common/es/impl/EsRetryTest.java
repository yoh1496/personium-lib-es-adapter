/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.common.es.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.transport.NodeDisconnectedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.personium.common.es.EsIndex;
import io.personium.common.es.EsType;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.PersoniumIndexResponse;

/**
 * EsTypeクラスのリトライテスト. 初版では、createメソッドのみ対応
 */
public class EsRetryTest extends EsTestBase {

    /**
     * 各テスト実行前の初期化処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * 各テスト実行後のクリーンアップ処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * ドキュメント新規作成時_初回でDocumentAlreadyExistsExceptionが発生した場合にEsClient_EsClientExceptionが返されること. ※
     * DocumentAlreadyExistsExceptionに限らず、初回処理でキャッチ対象でない例外が発生した場合の処理を対象としたテスト
     */
    @Test(expected = EsClientException.class)
    public void ドキュメント新規作成時_初回でDocumentAlreadyExistsExceptionが発生した場合にEsClient_EsClientExceptionが返されること() {
        EsIndex index = esClient.idxUser(INDEX_FOR_TEST, EsIndex.CATEGORY_AD);
        try {
            index.delete();
            index.create();
            EsType type = esClient.type("index_for_test_" + EsIndex.CATEGORY_AD,
                    "TypeForTest", "TestRoutingId", 5, 500);
            EsTypeImpl esTypeObject = (EsTypeImpl) Mockito.spy(type);

            // EsType#asyncIndex()が呼ばれた場合に、DocumentAlreadyExistsExceptionを投げる。
            // 送出する例外オブジェクトのモックを作成
            VersionConflictEngineException toBeThrown = Mockito.mock(VersionConflictEngineException.class);
            Mockito.doThrow(toBeThrown)
                    .when(esTypeObject)
                    .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                            Mockito.any(OpType.class), Mockito.anyLong());
            // メソッド呼び出し
            esTypeObject.create("dummyId", null);
            fail("EsClientException should be thrown.");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally { // CHECKSTYLE IGNORE
            //index.delete();
        }
    }

    /**
     * ドキュメント新規作成時_初回でIndexMissingExceptionが発生した場合にEsClient_EsIndexMissingExceptionが返されること.
     */
    @Test(expected = EsClientException.EsIndexMissingException.class)
    public void ドキュメント新規作成時_初回でIndexMissingExceptionが発生した場合にEsClient_EsIndexMissingExceptionが返されること() {
        //PowerMockito.mockStatic(EsClientException.class);
        EsTypeImpl esTypeObject = Mockito.spy(new EsTypeImpl("dummy", "Test", "TestRoutingId", 0, 0, null));

        // EsType#asyncIndex()が呼ばれた場合に、IndexMissingExceptionを投げる。
        // 送出する例外オブジェクトのモックを作成
        IndexNotFoundException toBeThrown = new IndexNotFoundException("dummy");
        Mockito.doThrow(toBeThrown)
                .when(esTypeObject)
                .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                        Mockito.any(OpType.class), Mockito.anyLong());
        // メソッド呼び出し
        esTypeObject.create("dummyId", null);
        fail("EsIndexMissingException should be thrown.");
    }

    /**
     * ドキュメント新規作成時_初回で根本例外IndexMissingExceptionが発生した場合にEsClient_EsIndexMissingExceptionが返されること.
     */
    @Test(expected = EsClientException.EsIndexMissingException.class)
    public void ドキュメント新規作成時_初回で根本例外IndexMissingExceptionが発生した場合にEsClient_EsIndexMissingExceptionが返されること() {
        // PowerMockito.mockStatic(EsClientException.class);
        EsTypeImpl esTypeObject = Mockito.spy(new EsTypeImpl("dummy", "Test", "TestRoutingId", 0, 0, null));
        // EsType#asyncIndex()が呼ばれた場合に、根本例外にIndexMissingExceptionを含むElasticsearchExceptionを投げる。
        ElasticsearchException toBeThrown = new ElasticsearchException("dummy", new IndexNotFoundException("foo"));
        Mockito.doThrow(toBeThrown)
                .when(esTypeObject)
                .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                        Mockito.any(OpType.class), Mockito.anyLong());
        esTypeObject.create("dummyId", null);
        fail("EsIndexMissingException should be thrown.");
    }

    /**
     * ドキュメント新規作成時_初回でMapperParsingExceptionが発生した場合にEsClient_EsSchemaMismatchExceptionが返されること.
     */
    @Test(expected = EsClientException.EsSchemaMismatchException.class)
    public void ドキュメント新規作成時_初回でMapperParsingExceptionが発生した場合にEsClient_EsSchemaMismatchExceptionが返されること() {
        //PowerMockito.mockStatic(EsClientException.class);
        EsTypeImpl esTypeObject = Mockito.spy(new EsTypeImpl("dummy", "Test", "TestRoutingId", 0, 0, null));

        // EsType#asyncIndex()が呼ばれた場合に、MapperParsingExceptionを投げる。
        // 送出する例外オブジェクトのモックを作成
        MapperParsingException toBeThrown = Mockito.mock(MapperParsingException.class);
        Mockito.doThrow(toBeThrown)
                .when(esTypeObject)
                .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                        Mockito.any(OpType.class), Mockito.anyLong());
        // メソッド呼び出し
        esTypeObject.create("dummyId", new HashMap<Object, Object>());
        fail("EsSchemaMismatchException should be thrown.");
    }

    /**
     * ドキュメント新規作成時_初回NodeDisconnectedExceptionが発生した場合にリトライを繰り返し最終的にEsClient_EsNoResponseExceptionが返されること.
     */
    @Test(expected = EsClientException.EsNoResponseException.class)
    public void ドキュメント新規作成時_初回NodeDisconnectedExceptionが発生した場合にリトライを繰り返し最終的にEsClient_EsNoResponseExceptionが返されること() {
        //PowerMockito.mockStatic(EsClientException.class);
        EsTypeImpl esTypeObject = Mockito.spy(new EsTypeImpl("dummy", "Test", "TestRoutingId", 5, 500, null));

        // EsType#asyncIndex()が呼ばれた場合に、NodeDisconnectedExceptionを投げる。
        // 送出する例外オブジェクトのモックを作成
        NodeDisconnectedException toBeThrown = Mockito.mock(NodeDisconnectedException.class);
        Mockito.doThrow(toBeThrown)
                .when(esTypeObject)
                .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                        Mockito.any(OpType.class), Mockito.anyLong());
        // メソッド呼び出し
        esTypeObject.create("dummyId", null);
        fail("EsNoResponseException should be thrown.");
    }

    /**
     * ドキュメント新規作成時_初回NoNodeAvailableExceptionが発生した場合にリトライを繰り返し最終的にEsClient_EsNoResponseExceptionが返されること.
     */
    @Test(expected = EsClientException.EsNoResponseException.class)
    public void ドキュメント新規作成時_初回NoNodeAvailableExceptionが発生した場合にリトライを繰り返し最終的にEsClient_EsNoResponseExceptionが返されること() {
        //PowerMockito.mockStatic(EsClientException.class);
        EsTypeImpl esTypeObject = Mockito.spy(new EsTypeImpl("dummy", "Test", "TestRoutingId", 5, 500, null));

        // EsType#asyncIndex()が呼ばれた場合に、NodeDisconnectedExceptionを投げる。
        // 送出する例外オブジェクトのモックを作成
        NoNodeAvailableException toBeThrown = Mockito.mock(NoNodeAvailableException.class);
        Mockito.doThrow(toBeThrown)
                .when(esTypeObject)
                .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                        Mockito.any(OpType.class), Mockito.anyLong());
        // メソッド呼び出し
        esTypeObject.create("dummyId", null);
        fail("EsNoResponseException should be thrown.");
    }

    /**
     * ドキュメント新規作成時_リトライ処理初回でDocumentAlreadyExistsExceptionが返された場合に正常なIndexResponseが返されること.
     */
    @Test
    public void ドキュメント新規作成時_リトライ処理初回でDocumentAlreadyExistExceptionが返された場合に正常なIndexResponseが返されること() {
        EsIndex index = esClient.idxUser(INDEX_FOR_TEST, EsIndex.CATEGORY_AD);
        try {
            index.delete();
            index.create();
            EsType type = esClient.type("index_for_test_" + EsIndex.CATEGORY_AD,
                    "TypeForTest", "TestRoutingId", 5, 500);
            type.create("dummyId", new HashMap<Object, Object>());
            EsTypeImpl esTypeObject = (EsTypeImpl) Mockito.spy(type);

            // EsType#asyncIndex()が呼ばれた場合に、NodeDisconnectedExceptionを投げる。
            // 送出する例外オブジェクトのモックを作成
            NodeDisconnectedException esDisconnectedException = Mockito.mock(NodeDisconnectedException.class);
            VersionConflictEngineException documentAlreadyExists = Mockito.mock(VersionConflictEngineException.class);
            Mockito.doThrow(esDisconnectedException)
                    // 本来のリクエスト
                    .doThrow(documentAlreadyExists)
                    // リトライ1回目
                    .when(esTypeObject)
                    .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                            Mockito.any(OpType.class), Mockito.anyLong());
            // メソッド呼び出し
            PersoniumIndexResponse response = esTypeObject.create("dummyId", new HashMap<Object, Object>());
            assertNotNull(response);
            assertEquals("index_for_test_" + EsIndex.CATEGORY_AD, response.getIndex());
            assertEquals("dummyId", response.getId());
            assertEquals("TypeForTest", response.getType());
            assertEquals(1, response.getVersion());
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // CHECKSTYLE IGNORE
            //index.delete();
        }
    }

    /**
     * ドキュメント新規作成時_リトライ処理の最大回数終了時点でDocumentAlreadyExistsExceptionが返された場合に正常なIndexResponseが返されること.
     */
    @Test
    public void ドキュメント新規作成時_リトライ処理の最大回数終了時点でDocumentAlreadyExistExceptionが返された場合に正常なIndexResponseが返されること() {
        EsIndex index = esClient.idxUser(INDEX_FOR_TEST, EsIndex.CATEGORY_AD);
        try {
            index.delete();
            index.create();
            EsType type = esClient.type("index_for_test_" + EsIndex.CATEGORY_AD,
                    "TypeForTest", "TestRoutingId", 5, 500);
            type.create("dummyId", new HashMap<Object, Object>());
            EsTypeImpl esTypeObject = (EsTypeImpl) Mockito.spy(type);

            // EsType#asyncIndex()が呼ばれた場合に、NodeDisconnectedExceptionを投げる。

            // 送出する例外オブジェクトのモックを作成
            NodeDisconnectedException esDisconnectedException = Mockito.mock(NodeDisconnectedException.class);
            NoNodeAvailableException esNoNodeAvailableException = Mockito.mock(NoNodeAvailableException.class);
            VersionConflictEngineException documentAlreadyExists = Mockito.mock(VersionConflictEngineException.class);
            Mockito.doThrow(esDisconnectedException)
                    // 本来のリクエスト時の例外
                    .doThrow(esNoNodeAvailableException)
                    // リトライ１回目
                    .doThrow(esNoNodeAvailableException)
                    // リトライ2回目
                    .doThrow(esNoNodeAvailableException)
                    // リトライ3回目
                    .doThrow(esNoNodeAvailableException)
                    // リトライ4回目
                    .doThrow(documentAlreadyExists)
                    .when(esTypeObject)
                    .asyncIndex(Mockito.anyString(), Mockito.anyMapOf(String.class, Object.class),
                            Mockito.any(OpType.class), Mockito.anyLong());
            // メソッド呼び出し
            PersoniumIndexResponse response = esTypeObject.create("dummyId", new HashMap<Object, Object>());
            assertNotNull(response);
            assertEquals("index_for_test_" + EsIndex.CATEGORY_AD, response.getIndex());
            assertEquals("dummyId", response.getId());
            assertEquals("TypeForTest", response.getType());
            assertEquals(1, response.getVersion());
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // CHECKSTYLE IGNORE
            //index.delete();
        }
    }

}
