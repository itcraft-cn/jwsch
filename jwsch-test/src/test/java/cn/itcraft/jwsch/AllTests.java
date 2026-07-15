package cn.itcraft.jwsch;

import cn.itcraft.jwsch.cli.CliTestSuite;
import cn.itcraft.jwsch.common.CommonTestSuite;
import cn.itcraft.jwsch.srv.SrvTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 全量测试套件。
 * 
 * <p>包含所有模块的单元测试，用于覆盖率统计。
 * 
 * <p>运行方式：
 * <pre>
 * mvn test -Dtest=AllTests
 * </pre>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    CommonTestSuite.class,
    CliTestSuite.class,
    SrvTestSuite.class
})
public class AllTests {
}