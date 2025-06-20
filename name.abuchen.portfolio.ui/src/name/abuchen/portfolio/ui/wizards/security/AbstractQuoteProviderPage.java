package name.abuchen.portfolio.ui.wizards.security;

import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.BinanceQuoteFeed;
import name.abuchen.portfolio.online.impl.BitfinexQuoteFeed;
import name.abuchen.portfolio.online.impl.CSQuoteFeed;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.ECBStatisticalDataWarehouseQuoteFeed;
import name.abuchen.portfolio.online.impl.EODHistoricalDataQuoteFeed;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.GenericJSONQuoteFeed;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.online.impl.KrakenQuoteFeed;
import name.abuchen.portfolio.online.impl.LeewayQuoteFeed;
import name.abuchen.portfolio.online.impl.MFAPIQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataQuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.swt.ControlDecoration;

public abstract class AbstractQuoteProviderPage extends AbstractPage
{
    private static final String YAHOO = "YAHOO"; //$NON-NLS-1$
    private static final String HTML = "HTML"; //$NON-NLS-1$

    private ComboViewer comboProvider;

    private Group grpQuoteFeed;
    private Label labelDetailData;

    private ComboViewer comboExchange;
    private Text textFeedURL;
    private Text textTicker;

    private Text textQuandlCode;
    private Label labelQuandlCloseColumnName;
    private Text textQuandlCloseColumnName;

    private Label labelJsonPathDate;
    private Text textJsonPathDate;
    private Label labelJsonPathClose;
    private Text textJsonPathClose;
    private Label labelJsonDateFormat;
    private Text textJsonDateFormat;
    private Label labelJsonDateTimezone;
    private Text textJsonDateTimezone;
    private Label labelJsonPathLow;
    private Text textJsonPathLow;
    private Label labelJsonPathHigh;
    private Text textJsonPathHigh;
    private Label labelJsonFactor;
    private Text textJsonFactor;
    private Label labelJsonPathVolume;
    private Text textJsonPathVolume;

    private Label labelCoinGeckoCoinId;
    private Text textCoinGeckoCoinId;

    private Text textSchemeCode;

    private PropertyChangeListener tickerSymbolPropertyChangeListener = e -> onTickerSymbolChanged();

    private final EditSecurityModel model;
    private final EditSecurityCache cache;
    private final BindingHelper bindings;

    // used to identify if the ticker has been changed on another page
    private String tickerSymbol;
    // used to identify if the currency has been changed on another page
    private String currencyCode;

    protected AbstractQuoteProviderPage(EditSecurityModel model, EditSecurityCache cache, BindingHelper bindings)
    {
        this.model = model;
        this.cache = cache;
        this.bindings = bindings;
    }

    protected final EditSecurityModel getModel()
    {
        return model;
    }

    protected abstract String getFeed();

    protected abstract void setFeed(String feed);

    protected abstract String getFeedURL();

    protected abstract void setFeedURL(String feedURL);

    protected abstract String getJSONDatePropertyName();

    protected abstract String getJSONDateFormatPropertyName();

    protected abstract String getJSONDateTimezonePropertyName();

    protected abstract String getJSONClosePropertyName();

    protected abstract String getJSONLowPathPropertyName();

    protected abstract String getJSONHighPathPropertyName();

    protected abstract String getJSONFactorPropertyName();

    protected abstract String getJSONVolumePathPropertyName();

    protected abstract void setStatus(String status);

    protected abstract void createSampleArea(Composite parent);

    protected abstract List<QuoteFeed> getAvailableFeeds();

    protected abstract QuoteFeed getQuoteFeedProvider(String feedId);

    protected abstract void reinitCaches();

    protected abstract void clearSampleQuotes();

    protected abstract void showSampleQuotes(QuoteFeed feed, Exchange exchange);

    @Override
    public void beforePage()
    {
        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

        if (!Objects.equals(tickerSymbol, model.getTickerSymbol()))
        {
            this.tickerSymbol = model.getTickerSymbol();

            if (this.tickerSymbol.isEmpty() && feed != null && (feed.getId().startsWith(YAHOO) //
                            || feed.getId().equals(PortfolioPerformanceFeed.ID) //
                            || feed.getId().equals(LeewayQuoteFeed.ID) //
                            || feed.getId().equals(TwelveDataQuoteFeed.ID)))
            {
                setStatus(MessageFormat.format(Messages.MsgCheckMissingTickerSymbol, getTitle()));
            }

            // clear caches
            cache.clearExchanges();
            reinitCaches();
            updateExchangesDropdown(feed);
        }

        if (feed != null && feed.getId() != null && feed.getId().indexOf(HTML) >= 0)
        {
            if (getFeedURL() == null || getFeedURL().length() == 0)
                clearSampleQuotes();
            else
                showSampleQuotes(feed, null);
        }

        if ((CoinGeckoQuoteFeed.ID.equals(getFeed()) || PortfolioReportQuoteFeed.ID.equals(getFeed()))
                        && !Objects.equals(currencyCode, model.getCurrencyCode()))
        {
            // coin gecko and portfolio report additionally uses the currency to
            // retrieve quotes
            this.currencyCode = model.getCurrencyCode();
            showSampleQuotes(feed, null);
        }

        if (textQuandlCode != null && !textQuandlCode.getText()
                        .equals(model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME)))
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            textQuandlCode.setText(code != null ? code : ""); //$NON-NLS-1$
        }

        if (textQuandlCloseColumnName != null && !textQuandlCloseColumnName.getText()
                        .equals(model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME)))
        {
            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            textQuandlCloseColumnName.setText(columnName != null ? columnName : ""); //$NON-NLS-1$
        }

        if (textJsonPathDate != null
                        && !textJsonPathDate.getText().equals(model.getFeedProperty(getJSONDatePropertyName())))
        {
            String path = model.getFeedProperty(getJSONDatePropertyName());
            textJsonPathDate.setText(path != null ? path : ""); //$NON-NLS-1$
        }

        if (textJsonPathClose != null
                        && !textJsonPathClose.getText().equals(model.getFeedProperty(getJSONClosePropertyName())))
        {
            String path = model.getFeedProperty(getJSONClosePropertyName());
            textJsonPathClose.setText(path != null ? path : ""); //$NON-NLS-1$
        }

        if (textJsonDateFormat != null
                        && !textJsonDateFormat.getText().equals(model.getFeedProperty(getJSONDateFormatPropertyName())))
        {
            String dateFormat = model.getFeedProperty(getJSONDateFormatPropertyName());
            textJsonDateFormat.setText(dateFormat != null ? dateFormat : ""); //$NON-NLS-1$
        }

        if (textJsonDateTimezone != null && !textJsonDateTimezone.getText()
                        .equals(model.getFeedProperty(getJSONDateTimezonePropertyName())))
        {
            String dateTimezone = model.getFeedProperty(getJSONDateTimezonePropertyName());
            textJsonDateTimezone.setText(dateTimezone != null ? dateTimezone : ""); //$NON-NLS-1$
        }

        if (textJsonPathLow != null
                        && !textJsonPathLow.getText().equals(model.getFeedProperty(getJSONLowPathPropertyName())))
        {
            String lowPath = model.getFeedProperty(getJSONLowPathPropertyName());
            textJsonPathLow.setText(lowPath != null ? lowPath : ""); //$NON-NLS-1$
        }

        if (textJsonPathHigh != null
                        && !textJsonPathHigh.getText().equals(model.getFeedProperty(getJSONHighPathPropertyName())))
        {
            String highPath = model.getFeedProperty(getJSONHighPathPropertyName());
            textJsonPathHigh.setText(highPath != null ? highPath : ""); //$NON-NLS-1$
        }

        if (textJsonPathVolume != null
                        && !textJsonPathVolume.getText().equals(model.getFeedProperty(getJSONVolumePathPropertyName())))
        {
            String volumePath = model.getFeedProperty(getJSONVolumePathPropertyName());
            textJsonPathVolume.setText(volumePath != null ? volumePath : ""); //$NON-NLS-1$
        }

        if (textCoinGeckoCoinId != null && !textCoinGeckoCoinId.getText()
                        .equals(model.getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID)))
        {
            String coinId = model.getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID);
            textCoinGeckoCoinId.setText(coinId != null ? coinId : ""); //$NON-NLS-1$
        }

        if (textSchemeCode != null
                        && !textSchemeCode.getText().equals(model.getFeedProperty(MFAPIQuoteFeed.SCHEME_CODE)))
        {
            String schemeCode = model.getFeedProperty(MFAPIQuoteFeed.SCHEME_CODE);
            textSchemeCode.setText(schemeCode != null ? schemeCode : ""); //$NON-NLS-1$
        }
    }

    @Override
    public void afterPage()
    {
        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        setFeed(feed.getId());

        currencyCode = getModel().getCurrencyCode();

        if (comboExchange != null && feed.getId() != null && (feed.getId().startsWith(YAHOO) //
                        || feed.getId().equals(PortfolioPerformanceFeed.ID) //
                        || feed.getId().equals(EurostatHICPQuoteFeed.ID) //
                        || feed.getId().equals(LeewayQuoteFeed.ID) //
                        || feed.getId().equals(TwelveDataQuoteFeed.ID) //
                        || feed.getId().equals(ECBStatisticalDataWarehouseQuoteFeed.ID)))
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
            {
                model.setTickerSymbol(exchange.getId());
                tickerSymbol = exchange.getId();
                setFeedURL(null);
            }
        }
        else if (textFeedURL != null)
        {
            setFeedURL(textFeedURL.getText());
        }
    }

    @Override
    public final void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new FormLayout());

        createProviderGroup(container);

        Composite buttonArea = new Composite(container, SWT.NONE);
        buttonArea.setLayout(new RowLayout(SWT.VERTICAL));
        createAdditionalButtons(buttonArea);

        FormData data = new FormData();
        data.top = new FormAttachment(grpQuoteFeed, 20, SWT.TOP);
        data.left = new FormAttachment(grpQuoteFeed, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(grpQuoteFeed, -10, SWT.BOTTOM);
        buttonArea.setLayoutData(data);

        Composite sampleArea = new Composite(container, SWT.NONE);
        sampleArea.setLayout(new FillLayout());
        createSampleArea(sampleArea);

        data = new FormData();
        data.top = new FormAttachment(grpQuoteFeed, 5);
        data.left = new FormAttachment(0, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(100, -10);
        sampleArea.setLayoutData(data);

        setupInitialData();

        comboProvider.addSelectionChangedListener(this::onFeedProviderChanged);
    }

    protected void createAdditionalButtons(Composite container)
    {
    }

    /**
     * Builds a temporary {@link Security} from the currently selected values.
     *
     * @return {@link Security}
     */
    protected Security buildTemporarySecurity()
    {
        // create a temporary security and set all attributes
        Security security = new Security(null, null);
        model.setAttributes(security);
        return security;
    }

    private void createProviderGroup(Composite container)
    {
        grpQuoteFeed = new Group(container, SWT.NONE);
        grpQuoteFeed.setText(Messages.LabelQuoteFeed);
        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 5);
        formData.left = new FormAttachment(0, 10);
        grpQuoteFeed.setLayoutData(formData);
        GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(5, 15, 5, 5).applyTo(grpQuoteFeed);

        Label lblProvider = new Label(grpQuoteFeed, SWT.NONE);
        lblProvider.setText(Messages.LabelQuoteFeedProvider);

        comboProvider = SWTHelper.createComboViewer(grpQuoteFeed);
        comboProvider.setContentProvider(ArrayContentProvider.getInstance());
        comboProvider.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((QuoteFeed) element).getName();
            }
        });
        comboProvider.setInput(getAvailableFeeds());
        GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboProvider.getControl());

        Link link = new Link(grpQuoteFeed, SWT.UNDERLINE_LINK);
        link.setText("<a>" + Messages.IntroLabelHelp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            try
            {
                QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

                String url = "https://help.portfolio-performance.info/de/how-to/kursdaten_laden/"; //$NON-NLS-1$

                if (feed != null && feed.getHelpURL().isPresent())
                    url = feed.getHelpURL().get();

                // Use Google translate for non-German users (as the help pages
                // are currently only available in German). Taking care to
                // encode the #.
                if (!Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()))
                    url = MessageFormat.format(Messages.HelpURL, URLEncoder.encode(url, StandardCharsets.UTF_8.name()));

                DesktopAPI.browse(url);
            }
            catch (UnsupportedEncodingException ignore)
            {
                // UTF-8 is supported
            }
        }));

        labelDetailData = new Label(grpQuoteFeed, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 5).applyTo(labelDetailData);

        createDetailDataWidgets(null);
    }

    private void createDetailDataWidgets(QuoteFeed feed)
    {
        boolean dropDown = feed != null && feed.getId() != null && //
                        (feed.getId().startsWith(YAHOO) //
                                        || feed.getId().equals(PortfolioPerformanceFeed.ID) //
                                        || feed.getId().equals(EurostatHICPQuoteFeed.ID) //
                                        || feed.getId().equals(ECBStatisticalDataWarehouseQuoteFeed.ID) //
                                        || feed.getId().equals(LeewayQuoteFeed.ID) //
                                        || feed.getId().equals(TwelveDataQuoteFeed.ID));

        boolean feedURL = feed != null && feed.getId() != null && //
                        (feed.getId().equals(HTMLTableQuoteFeed.ID) //
                                        || feed.getId().equals(CSQuoteFeed.ID) //
                                        || feed.getId().equals(GenericJSONQuoteFeed.ID));

        boolean needsTicker = feed != null && feed.getId() != null //
                        && Set.of(AlphavantageQuoteFeed.ID, //
                                        FinnhubQuoteFeed.ID, //
                                        BinanceQuoteFeed.ID, //
                                        BitfinexQuoteFeed.ID, //
                                        CoinGeckoQuoteFeed.ID, //
                                        KrakenQuoteFeed.ID, //
                                        EODHistoricalDataQuoteFeed.ID) //
                                        .contains(feed.getId());

        boolean needsQuandlCode = feed != null && feed.getId() != null && feed.getId().equals(QuandlQuoteFeed.ID);

        boolean needsJsonPath = feed != null && feed.getId() != null && feed.getId().equals(GenericJSONQuoteFeed.ID);

        boolean needsCoinGeckoCoinId = feed != null && feed.getId() != null
                        && feed.getId().equals(CoinGeckoQuoteFeed.ID);

        boolean needsSchemeCode = feed != null && feed.getId() != null && feed.getId().equals(MFAPIQuoteFeed.ID);

        if (textFeedURL != null)
        {
            textFeedURL.dispose();
            textFeedURL = null;
        }

        if (comboExchange != null)
        {
            comboExchange.getControl().dispose();
            comboExchange = null;
        }

        if (textTicker != null)
        {
            textTicker.dispose();
            textTicker = null;

            model.removePropertyChangeListener("tickerSymbol", tickerSymbolPropertyChangeListener); //$NON-NLS-1$
        }

        textQuandlCode = disposeIf(textQuandlCode);
        labelQuandlCloseColumnName = disposeIf(labelQuandlCloseColumnName);
        textQuandlCloseColumnName = disposeIf(textQuandlCloseColumnName);

        labelJsonPathDate = disposeIf(labelJsonPathDate);
        textJsonPathDate = disposeIf(textJsonPathDate);
        labelJsonPathClose = disposeIf(labelJsonPathClose);
        textJsonPathClose = disposeIf(textJsonPathClose);
        labelJsonDateFormat = disposeIf(labelJsonDateFormat);
        textJsonDateFormat = disposeIf(textJsonDateFormat);
        labelJsonDateTimezone = disposeIf(labelJsonDateTimezone);
        textJsonDateTimezone = disposeIf(textJsonDateTimezone);
        labelJsonPathLow = disposeIf(labelJsonPathLow);
        textJsonPathLow = disposeIf(textJsonPathLow);
        labelJsonPathHigh = disposeIf(labelJsonPathHigh);
        textJsonPathHigh = disposeIf(textJsonPathHigh);
        labelJsonFactor = disposeIf(labelJsonFactor);
        textJsonFactor = disposeIf(textJsonFactor);
        labelJsonPathVolume = disposeIf(labelJsonPathVolume);
        textJsonPathVolume = disposeIf(textJsonPathVolume);

        labelCoinGeckoCoinId = disposeIf(labelCoinGeckoCoinId);
        textCoinGeckoCoinId = disposeIf(textCoinGeckoCoinId);

        textSchemeCode = disposeIf(textSchemeCode);

        if (dropDown)
        {
            labelDetailData.setText(Messages.LabelExchange);

            comboExchange = new ComboViewer(grpQuoteFeed, SWT.READ_ONLY);
            comboExchange.setContentProvider(ArrayContentProvider.getInstance());
            comboExchange.setLabelProvider(new LabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    Exchange exchange = (Exchange) element;
                    return exchange.getDisplayName();
                }
            });
            GridDataFactory.fillDefaults().span(2, 1).hint(300, SWT.DEFAULT).applyTo(comboExchange.getControl());

            comboExchange.addSelectionChangedListener(this::onExchangeChanged);
        }

        if (feedURL)
        {
            labelDetailData.setText(Messages.EditWizardQuoteFeedLabelFeedURL);

            textFeedURL = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(300, SWT.DEFAULT).applyTo(textFeedURL);

            textFeedURL.addModifyListener(e -> onFeedURLChanged());
        }

        if (needsTicker)
        {
            labelDetailData.setText(Messages.ColumnTicker);

            textTicker = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textTicker);

            IObservableValue<?> observeText = WidgetProperties.text(SWT.Modify).observe(textTicker);
            IObservableValue<?> observable = BeanProperties.value("tickerSymbol").observe(model); //$NON-NLS-1$
            bindings.getBindingContext().bindValue(observeText, observable);

            model.addPropertyChangeListener("tickerSymbol", tickerSymbolPropertyChangeListener); //$NON-NLS-1$
        }

        if (needsQuandlCode)
        {
            labelDetailData.setText(Messages.LabelQuandlCode);

            textQuandlCode = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textQuandlCode);
            textQuandlCode.addModifyListener(e -> onQuandlCodeChanged());

            labelQuandlCloseColumnName = new Label(grpQuoteFeed, SWT.NONE);
            labelQuandlCloseColumnName.setText(Messages.LabelQuandlColumnNameQuote);

            textQuandlCloseColumnName = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textQuandlCloseColumnName);

            ControlDecoration deco = new ControlDecoration(textQuandlCloseColumnName, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelQuandlColumnNameQuoteHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            textQuandlCloseColumnName.addModifyListener(e -> onQuandlColumnNameChanged());
        }

        if (needsJsonPath)
        {
            labelJsonPathDate = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathDate.setText(Messages.LabelJSONPathToDate);

            textJsonPathDate = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathDate);
            textJsonPathDate.addModifyListener(e -> onJsonPathDateChanged());

            ControlDecoration deco = new ControlDecoration(textJsonPathDate, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonDateFormat = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonDateFormat.setText(Messages.LabelJSONDateFormat);

            textJsonDateFormat = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonDateFormat);
            textJsonDateFormat.addModifyListener(e -> onJsonDateFormatChanged());

            deco = new ControlDecoration(textJsonDateFormat, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONDateFormatHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonDateTimezone = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonDateTimezone.setText(Messages.LabelJSONDateTimezone);

            textJsonDateTimezone = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonDateTimezone);
            textJsonDateTimezone.addModifyListener(e -> onJsonDateTimezoneChanged());

            deco = new ControlDecoration(textJsonDateTimezone, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONDateTimezoneHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonPathClose = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathClose.setText(Messages.LabelJSONPathToClose);

            textJsonPathClose = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathClose);
            textJsonPathClose.addModifyListener(e -> onJsonPathCloseChanged());

            deco = new ControlDecoration(textJsonPathClose, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonPathLow = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathLow.setText(Messages.LabelJSONPathToLow);

            textJsonPathLow = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathLow);
            textJsonPathLow.addModifyListener(e -> onJsonPathLowChanged());

            deco = new ControlDecoration(textJsonPathLow, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonPathHigh = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathHigh.setText(Messages.LabelJSONPathToHigh);

            textJsonPathHigh = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathHigh);
            textJsonPathHigh.addModifyListener(e -> onJsonPathHighChanged());

            deco = new ControlDecoration(textJsonPathHigh, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonFactor = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonFactor.setText(Messages.LabelJSONFactor);

            textJsonFactor = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonFactor);
            textJsonFactor.addModifyListener(e -> onJsonFactorChanged());

            deco = new ControlDecoration(textJsonFactor, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONFactorHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonPathVolume = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathVolume.setText(Messages.LabelJSONPathToVolume);

            textJsonPathVolume = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathVolume);
            textJsonPathVolume.addModifyListener(e -> onJsonPathVolumeChanged());

            deco = new ControlDecoration(textJsonPathVolume, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();
        }

        if (needsCoinGeckoCoinId)
        {
            labelCoinGeckoCoinId = new Label(grpQuoteFeed, SWT.NONE);
            labelCoinGeckoCoinId.setText(Messages.LabelCoinGeckoCoinId);

            textCoinGeckoCoinId = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textCoinGeckoCoinId);
            textCoinGeckoCoinId.addModifyListener(e -> onCoinGeckoCoinIdChanged());

            ControlDecoration deco = new ControlDecoration(textCoinGeckoCoinId, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelCoinGeckoCoinIdHint);
            deco.setMarginWidth(2);
            deco.show();
        }

        if (needsSchemeCode)
        {
            labelDetailData.setText("Scheme Code"); //$NON-NLS-1$

            textSchemeCode = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textSchemeCode);
            textSchemeCode.addModifyListener(e -> onSchemeCodeChanged());
        }

        if (!dropDown && !feedURL && !needsTicker && !needsQuandlCode && !needsJsonPath && !needsCoinGeckoCoinId
                        && !needsSchemeCode)
        {
            labelDetailData.setText(""); //$NON-NLS-1$
        }

        grpQuoteFeed.layout(true);
        grpQuoteFeed.getParent().layout();
    }

    private <T extends Control> T disposeIf(T control)
    {
        if (control != null && !control.isDisposed())
            control.dispose();
        return null;
    }

    private void setupInitialData()
    {
        this.tickerSymbol = model.getTickerSymbol();

        QuoteFeed feed = getQuoteFeedProvider(getFeed());

        // register listener when exchanges have been loaded

        cache.addListener((quoteFeed, exchanges) -> {
            if (comboExchange == null)
                return;

            var currentFeed = (QuoteFeed) comboProvider.getStructuredSelection().getFirstElement();
            if (!Objects.equals(currentFeed, quoteFeed))
                return;

            updateExchangesDropdown(quoteFeed, exchanges);
        });

        if (feed != null)
            comboProvider.setSelection(new StructuredSelection(feed));

        // check if the feed was available for selection in drop down box
        if (feed == null || comboProvider.getSelection().isEmpty())
            comboProvider.setSelection(new StructuredSelection(getAvailableFeeds().get(0)));

        createDetailDataWidgets(feed);

        if (model.getTickerSymbol() != null && feed != null && feed.getId() != null && //
                        (feed.getId().startsWith(YAHOO) //
                                        || feed.getId().equals(PortfolioPerformanceFeed.ID) //
                                        || feed.getId().equals(LeewayQuoteFeed.ID) //
                                        || feed.getId().equals(TwelveDataQuoteFeed.ID) //
                                        || feed.getId().equals(EurostatHICPQuoteFeed.ID) //
                                        || feed.getId().equals(ECBStatisticalDataWarehouseQuoteFeed.ID)))
        {
            updateExchangesDropdown(feed);
        }

        if (textFeedURL != null && getFeedURL() != null)
        {
            textFeedURL.setText(getFeedURL());
        }

        if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
        }

        if (textJsonPathDate != null)
        {
            String datePath = model.getFeedProperty(getJSONDatePropertyName());
            if (datePath != null)
                textJsonPathDate.setText(datePath);

            String closePath = model.getFeedProperty(getJSONClosePropertyName());
            if (closePath != null)
                textJsonPathClose.setText(closePath);

            String dateFormat = model.getFeedProperty(getJSONDateFormatPropertyName());
            if (dateFormat != null)
                textJsonDateFormat.setText(dateFormat);

            String dateTimezone = model.getFeedProperty(getJSONDateTimezonePropertyName());
            if (dateTimezone != null)
                textJsonDateTimezone.setText(dateTimezone);

            String lowPath = model.getFeedProperty(getJSONLowPathPropertyName());
            if (lowPath != null)
                textJsonPathLow.setText(lowPath);

            String highPath = model.getFeedProperty(getJSONHighPathPropertyName());
            if (highPath != null)
                textJsonPathHigh.setText(highPath);

            String factor = model.getFeedProperty(getJSONFactorPropertyName());
            if (factor != null)
                textJsonFactor.setText(factor);

            String volumePath = model.getFeedProperty(getJSONVolumePathPropertyName());
            if (volumePath != null)
                textJsonPathVolume.setText(volumePath);
        }

        if (textCoinGeckoCoinId != null)
        {
            String coinId = model.getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID);
            if (coinId != null)
                textCoinGeckoCoinId.setText(coinId);
        }

        if (textSchemeCode != null)
        {
            String schemeCode = model.getFeedProperty(MFAPIQuoteFeed.SCHEME_CODE);
            if (schemeCode != null)
                textSchemeCode.setText(schemeCode);
        }
    }

    /**
     * Updates the dropdown by loading the exchanges from the cache. If the list
     * is not (yet) cached, then the current ticker is set as single element in
     * the dropdown to have a valid selection.
     */
    private void updateExchangesDropdown(QuoteFeed feed)
    {
        if (feed != null && comboExchange != null)
        {
            var exchanges = cache.getOrLoadExchanges(feed, buildTemporarySecurity());

            if (exchanges.isPresent())
            {
                var listOfExchanges = exchanges.get();

                updateExchangesDropdown(feed, listOfExchanges);
            }
            else
            {
                // the job to load exchanges it scheduled. Until then display
                // the existing symbol if available.

                if (this.tickerSymbol != null && !this.tickerSymbol.isEmpty())
                {
                    Exchange exchange = new Exchange(this.tickerSymbol, this.tickerSymbol);
                    ArrayList<Exchange> input = new ArrayList<>();
                    input.add(exchange);
                    comboExchange.setInput(input);
                    comboExchange.setSelection(new StructuredSelection(exchange));
                    setStatus(null);
                }
                else
                {
                    comboExchange.setInput(new ArrayList<>());
                    comboExchange.setSelection(StructuredSelection.EMPTY);
                    setStatus(MessageFormat.format(Messages.MsgCheckMissingTickerSymbol, getTitle()));
                }
            }
        }
    }

    /**
     * Updates the dropdown with the list of exchanges. Keeps the previous
     * exchange id active if one is selected. Updates the status of the dialog.
     */
    private void updateExchangesDropdown(QuoteFeed feed, List<Exchange> exchanges)
    {
        if (comboExchange != null)
        {
            // remember the previous exchange id in order to select it again
            String previousExchangeId = null;
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
                previousExchangeId = exchange.getId();

            if (previousExchangeId == null && model.getTickerSymbol() != null)
            {
                previousExchangeId = model.getTickerSymbol();
            }

            // set new list of exchanges
            comboExchange.setInput(exchanges);

            // select exchange if other provider supports same exchange id
            // (yahoo close vs. yahoo adjusted close)
            boolean exchangeSelected = false;
            if (previousExchangeId != null)
            {
                for (Exchange e : exchanges)
                {
                    if (e.getId().equals(previousExchangeId))
                    {
                        comboExchange.setSelection(new StructuredSelection(e));
                        exchangeSelected = true;
                        break;
                    }
                }
            }

            if (!exchangeSelected && exchanges.size() == 1)
            {
                comboExchange.setSelection(new StructuredSelection(exchanges.get(0)));
                exchangeSelected = true;
            }

            if (!exchangeSelected)
                comboExchange.setSelection(StructuredSelection.EMPTY);

            if (this.tickerSymbol == null || this.tickerSymbol.isEmpty() && //
                            (feed.getId().startsWith(YAHOO) //
                                            || feed.getId().equals(PortfolioPerformanceFeed.ID) //
                                            || feed.getId().equals(LeewayQuoteFeed.ID) //
                                            || feed.getId().equals(TwelveDataQuoteFeed.ID)))
            {
                setStatus(MessageFormat.format(Messages.MsgCheckMissingTickerSymbol, getTitle()));
            }
            else
            {
                setStatus(comboExchange.getStructuredSelection().isEmpty()
                                ? MessageFormat.format(Messages.MsgErrorExchangeMissing, getTitle())
                                : null);
            }
        }
    }

    private void onFeedProviderChanged(SelectionChangedEvent event)
    {
        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) event.getSelection()).getFirstElement();
        if (feed != null)
            setFeed(feed.getId());

        createDetailDataWidgets(feed);

        clearSampleQuotes();

        if (comboExchange != null)
        {
            updateExchangesDropdown(feed);
        }

        if (textFeedURL != null)
        {
            boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

            if (hasURL)
                textFeedURL.setText(getFeedURL());

            setStatus(hasURL ? null : MessageFormat.format(Messages.EditWizardQuoteFeedMsgErrorMissingURL, getTitle()));
        }

        if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
        }

        if (textJsonPathDate != null)
        {
            String datePath = model.getFeedProperty(getJSONDatePropertyName());
            if (datePath != null)
                textJsonPathDate.setText(datePath);

            String closePath = model.getFeedProperty(getJSONClosePropertyName());
            if (closePath != null)
                textJsonPathClose.setText(closePath);

            String dateFormat = model.getFeedProperty(getJSONDateFormatPropertyName());
            if (dateFormat != null)
                textJsonDateFormat.setText(dateFormat);

            String dateTimezone = model.getFeedProperty(getJSONDateTimezonePropertyName());
            if (dateTimezone != null)
                textJsonDateTimezone.setText(dateTimezone);

            String lowPath = model.getFeedProperty(getJSONLowPathPropertyName());
            if (lowPath != null)
                textJsonPathLow.setText(lowPath);

            String highPath = model.getFeedProperty(getJSONHighPathPropertyName());
            if (highPath != null)
                textJsonPathHigh.setText(highPath);

            String factor = model.getFeedProperty(getJSONFactorPropertyName());
            if (factor != null)
                textJsonFactor.setText(factor);

            String volumePath = model.getFeedProperty(getJSONVolumePathPropertyName());
            if (volumePath != null)
                textJsonPathVolume.setText(volumePath);
        }

        if (textCoinGeckoCoinId != null)
        {
            String coinId = model.getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID);
            if (coinId != null)
                textCoinGeckoCoinId.setText(coinId);
        }

        if (textSchemeCode != null)
        {
            String schemeCode = model.getFeedProperty(MFAPIQuoteFeed.SCHEME_CODE);
            if (schemeCode != null)
                textSchemeCode.setText(schemeCode);
        }

        if (comboExchange == null && textFeedURL == null && textQuandlCode == null && textJsonPathDate == null
                        && textCoinGeckoCoinId == null && textSchemeCode == null)
        {
            // get sample quotes?
            if (feed != null)
            {
                showSampleQuotes(feed, null);
            }
            else
            {
                clearSampleQuotes();
            }
            setStatus(null);
        }
    }

    private void onExchangeChanged(SelectionChangedEvent event)
    {
        Exchange exchange = (Exchange) ((IStructuredSelection) event.getSelection()).getFirstElement();
        setStatus(null);

        if (exchange == null)
        {
            clearSampleQuotes();
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, exchange);
        }
    }

    private void onFeedURLChanged()
    {
        setFeedURL(textFeedURL.getText());

        boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

        if (!hasURL)
        {
            clearSampleQuotes();
            setStatus(MessageFormat.format(Messages.EditWizardQuoteFeedMsgErrorMissingURL, getTitle()));
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onTickerSymbolChanged()
    {
        boolean hasTicker = model.getTickerSymbol() != null && !model.getTickerSymbol().isEmpty();

        if (!hasTicker)
        {
            clearSampleQuotes();
            setStatus(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTicker));
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onQuandlCodeChanged()
    {
        String quandlCode = textQuandlCode.getText();

        boolean hasCode = quandlCode != null && Pattern.matches("^.+/.+$", quandlCode); //$NON-NLS-1$

        if (!hasCode)
        {
            clearSampleQuotes();
            setStatus(Messages.MsgErrorMissingQuandlCode);
        }
        else
        {
            model.setFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME, quandlCode);
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onQuandlColumnNameChanged()
    {
        String closeColumnName = textQuandlCloseColumnName.getText();

        model.setFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME,
                        closeColumnName.isEmpty() ? null : closeColumnName);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathDateChanged()
    {
        String datePath = textJsonPathDate.getText();

        model.setFeedProperty(getJSONDatePropertyName(), datePath.isEmpty() ? null : datePath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathCloseChanged()
    {
        String closePath = textJsonPathClose.getText();

        model.setFeedProperty(getJSONClosePropertyName(), closePath.isEmpty() ? null : closePath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonDateFormatChanged()
    {
        String dateFormat = textJsonDateFormat.getText();

        model.setFeedProperty(getJSONDateFormatPropertyName(), dateFormat.isEmpty() ? null : dateFormat);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonDateTimezoneChanged()
    {
        String dateTimezone = textJsonDateTimezone.getText();

        model.setFeedProperty(getJSONDateTimezonePropertyName(), dateTimezone.isEmpty() ? null : dateTimezone);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathLowChanged()
    {
        String lowPath = textJsonPathLow.getText();

        model.setFeedProperty(getJSONLowPathPropertyName(), lowPath.isEmpty() ? null : lowPath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathHighChanged()
    {
        String highPath = textJsonPathHigh.getText();

        model.setFeedProperty(getJSONHighPathPropertyName(), highPath.isEmpty() ? null : highPath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonFactorChanged()
    {
        String factor = textJsonFactor.getText();

        model.setFeedProperty(getJSONFactorPropertyName(), factor.isEmpty() ? null : factor);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathVolumeChanged()
    {
        String volumePath = textJsonPathVolume.getText();

        model.setFeedProperty(getJSONVolumePathPropertyName(), volumePath.isEmpty() ? null : volumePath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onCoinGeckoCoinIdChanged()
    {
        String coinId = textCoinGeckoCoinId.getText();

        model.setFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, coinId.isEmpty() ? null : coinId);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onSchemeCodeChanged()
    {
        String schemeCode = textSchemeCode.getText();

        model.setFeedProperty(MFAPIQuoteFeed.SCHEME_CODE, schemeCode.isEmpty() ? null : schemeCode);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }
}
