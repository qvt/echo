package pt.uminho.haslab.echo.transform.kodkod;

import pt.uminho.haslab.echo.EngineRunner;
import pt.uminho.haslab.echo.transform.EchoTranslator;
import pt.uminho.haslab.echo.transform.TransformFactory;

/**
 * Created with IntelliJ IDEA.
 * User: tmg
 * Date: 10/24/13
 * Time: 12:31 PM
 */
public class KodkodFactory implements TransformFactory {
    @Override
    public EngineRunner createRunner() {
        return new KodkodRunner();
    }

    @Override
    public EchoTranslator createTranslator() {
        return new KodkodEchoTranslator();  //To change body of implemented methods use File | Settings | File Templates.
    }
}
