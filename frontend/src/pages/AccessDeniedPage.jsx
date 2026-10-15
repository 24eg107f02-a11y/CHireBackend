import { Link } from "react-router-dom";

export default function AccessDeniedPage() {
  return <section className="message-page"><span className="message-code">403</span><h1>This space has a different key.</h1><p>Your current account role does not have permission to open this page.</p><Link className="button button-primary" to="/dashboard">Go to my dashboard</Link></section>;
}
