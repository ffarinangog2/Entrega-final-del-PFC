import { useTranslation } from 'react-i18next'
import { DashboardLayout } from '../components/DashboardLayout'
import '../i18n'
import './AboutPage.css'

const APP_VERSION = '0.1.0'

export function AboutPage() {
  const { t } = useTranslation()

  return (
    <DashboardLayout breadcrumb={t('about.title')}>
      <div className="about-page">
        <h1>{t('about.title')}</h1>
        <p className="about-page__subtitle">{t('about.subtitle')}</p>

        <dl className="about-page__facts">
          <div className="about-page__fact">
            <dt>{t('about.appNameLabel')}</dt>
            <dd>{t('about.appName')}</dd>
          </div>
          <div className="about-page__fact">
            <dt>{t('about.versionLabel')}</dt>
            <dd>{APP_VERSION}</dd>
          </div>
          <div className="about-page__fact">
            <dt>{t('about.stackLabel')}</dt>
            <dd>{t('about.stackValue')}</dd>
          </div>
        </dl>
      </div>
    </DashboardLayout>
  )
}
